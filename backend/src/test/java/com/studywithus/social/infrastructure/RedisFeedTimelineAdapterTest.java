package com.studywithus.social.infrastructure;

import com.studywithus.social.domain.FeedTimelineEntry;
import com.studywithus.social.domain.FeedTimelineSlice;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisFeedTimelineAdapterTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    private SimpleMeterRegistry meterRegistry;
    private RedisFeedTimelineAdapter adapter;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        adapter = new RedisFeedTimelineAdapter(redisTemplate, meterRegistry, true, 1000, 7);
    }

    @Test
    void shouldReturnCacheMissWhenRedisTemplateIsNull() {
        RedisFeedTimelineAdapter noRedisAdapter = new RedisFeedTimelineAdapter(null, meterRegistry, true, 1000, 7);
        FeedTimelineSlice slice = noRedisAdapter.getUserTimeline(UUID.randomUUID(), null, null, 20);
        assertThat(slice.cacheHit()).isFalse();
        assertThat(slice.checkinIds()).isEmpty();
    }

    @Test
    void shouldReturnCacheMissWhenKeyDoesNotExist() {
        UUID userId = UUID.randomUUID();
        when(redisTemplate.hasKey("feed:user:" + userId)).thenReturn(false);

        FeedTimelineSlice slice = adapter.getUserTimeline(userId, null, null, 20);

        assertThat(slice.cacheHit()).isFalse();
        assertThat(slice.checkinIds()).isEmpty();
    }

    @Test
    void shouldReturnFirstPageFromRedisZSetOnCacheHit() {
        UUID userId = UUID.randomUUID();
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        String key = "feed:user:" + userId;

        when(redisTemplate.hasKey(key)).thenReturn(true);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.reverseRange(key, 0, 1L)).thenReturn(new LinkedHashSet<>(List.of(id1.toString(), id2.toString())));

        FeedTimelineSlice slice = adapter.getUserTimeline(userId, null, null, 2);

        assertThat(slice.cacheHit()).isTrue();
        assertThat(slice.checkinIds()).containsExactly(id1, id2);
    }

    @Test
    void shouldPaginateWithCursorUsingReverseRank() {
        UUID communityId = UUID.randomUUID();
        UUID cursorId = UUID.randomUUID();
        UUID nextId = UUID.randomUUID();
        String key = "feed:community:" + communityId;
        Instant cursorTime = Instant.now();

        when(redisTemplate.hasKey(key)).thenReturn(true);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.reverseRank(key, cursorId.toString())).thenReturn(1L);
        when(zSetOperations.reverseRange(key, 2L, 3L)).thenReturn(Set.of(nextId.toString()));

        FeedTimelineSlice slice = adapter.getCommunityTimeline(communityId, cursorTime, cursorId, 2);

        assertThat(slice.cacheHit()).isTrue();
        assertThat(slice.checkinIds()).containsExactly(nextId);
    }

    @Test
    void shouldFallbackToScoreRangeWhenCursorItemWasDeletedFromZSet() {
        UUID userId = UUID.randomUUID();
        UUID cursorId = UUID.randomUUID();
        UUID olderId = UUID.randomUUID();
        String key = "feed:user:" + userId;
        Instant cursorTime = Instant.now();

        when(redisTemplate.hasKey(key)).thenReturn(true);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.reverseRank(key, cursorId.toString())).thenReturn(null);
        when(zSetOperations.reverseRangeByScoreWithScores(
                eq(key),
                eq(Double.NEGATIVE_INFINITY),
                anyDouble(),
                eq(0L),
                anyLong()
        )).thenReturn(Set.of(
                new DefaultTypedTuple<>(olderId.toString(), (double) cursorTime.minusSeconds(10).toEpochMilli())
        ));

        FeedTimelineSlice slice = adapter.getUserTimeline(userId, cursorTime, cursorId, 5);

        assertThat(slice.cacheHit()).isTrue();
        assertThat(slice.checkinIds()).containsExactly(olderId);
    }

    @Test
    void shouldFallbackGracefullyAndIncrementMetricOnRedisConnectionFailure() {
        UUID userId = UUID.randomUUID();
        when(redisTemplate.hasKey("feed:user:" + userId))
                .thenThrow(new RedisConnectionFailureException("Connection refused"));

        FeedTimelineSlice slice = adapter.getUserTimeline(userId, null, null, 20);

        assertThat(slice.cacheHit()).isFalse();
        assertThat(slice.checkinIds()).isEmpty();
        assertThat(meterRegistry.counter("studywithus_feed_cache_fallbacks_total").count()).isEqualTo(1.0);
    }

    @Test
    void shouldExecutePipelinedWritesForFanOutAndRemovalAndWarming() {
        UUID checkinId = UUID.randomUUID();
        UUID communityId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        adapter.addCheckinToTimelines(checkinId, Instant.now(), communityId, List.of(userId));
        adapter.removeCheckinFromTimelines(checkinId, communityId, List.of(userId));
        adapter.warmUserTimeline(userId, List.of(new FeedTimelineEntry(checkinId, Instant.now())));
        adapter.invalidateUserTimeline(userId);

        verify(redisTemplate).delete("feed:user:" + userId);
        verify(redisTemplate, org.mockito.Mockito.times(3)).executePipelined(any(RedisCallback.class));
    }
}
