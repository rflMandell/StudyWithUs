package com.studywithus.league.infrastructure;

import com.studywithus.league.domain.LeagueLeaderboardCachePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisLeagueLeaderboardAdapterTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    private RedisLeagueLeaderboardAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new RedisLeagueLeaderboardAdapter(redisTemplate, true);
    }

    @Test
    void shouldReturnLeaderboardFromRedisWhenCached() {
        UUID poolId = UUID.randomUUID();
        UUID u1 = UUID.randomUUID();
        String key = "league:pool:" + poolId;

        when(redisTemplate.hasKey(key)).thenReturn(true);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.reverseRangeWithScores(key, 0, -1))
                .thenReturn(Set.of(new DefaultTypedTuple<>(u1.toString(), 120.0)));

        Optional<List<LeagueLeaderboardCachePort.LeaderboardScoreEntry>> result = adapter.getPoolLeaderboard(poolId);

        assertThat(result).isPresent();
        assertThat(result.get()).containsExactly(new LeagueLeaderboardCachePort.LeaderboardScoreEntry(u1, 120L));
    }

    @Test
    void shouldFallbackGracefullyOnRedisOutage() {
        UUID poolId = UUID.randomUUID();
        when(redisTemplate.hasKey("league:pool:" + poolId))
                .thenThrow(new RedisConnectionFailureException("Redis down"));

        Optional<List<LeagueLeaderboardCachePort.LeaderboardScoreEntry>> result = adapter.getPoolLeaderboard(poolId);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldUpsertAndInvalidatePool() {
        UUID poolId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);

        adapter.upsertScore(poolId, userId, 45L);
        adapter.invalidatePool(poolId);

        verify(zSetOperations).add("league:pool:" + poolId, userId.toString(), 45.0);
        verify(redisTemplate).delete("league:pool:" + poolId);
    }
}
