package com.studywithus.social.infrastructure;

import com.studywithus.social.domain.FeedTimelineEntry;
import com.studywithus.social.domain.FeedTimelinePort;
import com.studywithus.social.domain.FeedTimelineSlice;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
public class RedisFeedTimelineAdapter implements FeedTimelinePort {

    private static final String USER_TIMELINE_PREFIX = "feed:user:";
    private static final String COMMUNITY_TIMELINE_PREFIX = "feed:community:";

    private final StringRedisTemplate redisTemplate;
    private final boolean redisEnabled;
    private final int maxTimelineSize;
    private final long timelineTtlSeconds;
    private final Counter fallbackCounter;

    public RedisFeedTimelineAdapter(
            @Autowired(required = false) StringRedisTemplate redisTemplate,
            @Autowired(required = false) MeterRegistry meterRegistry,
            @Value("${app.feed.redis-enabled:true}") boolean redisEnabled,
            @Value("${app.feed.max-timeline-size:1000}") int maxTimelineSize,
            @Value("${app.feed.timeline-ttl-days:7}") long timelineTtlDays
    ) {
        this.redisTemplate = redisTemplate;
        this.redisEnabled = redisEnabled;
        this.maxTimelineSize = Math.max(maxTimelineSize, 50);
        this.timelineTtlSeconds = Duration.ofDays(Math.max(timelineTtlDays, 1)).toSeconds();
        this.fallbackCounter = meterRegistry != null
                ? Counter.builder("studywithus_feed_cache_fallbacks_total")
                        .description("Total number of feed requests that fell back to PostgreSQL due to Redis error")
                        .register(meterRegistry)
                : null;
    }

    @Override
    public void addCheckinToTimelines(UUID checkinId, Instant createdAt, UUID communityId, Collection<UUID> userIds) {
        if (!isAvailable() || checkinId == null || createdAt == null) {
            return;
        }

        List<String> targetKeys = buildTargetKeys(communityId, userIds);
        if (targetKeys.isEmpty()) {
            return;
        }

        double score = createdAt.toEpochMilli();
        byte[] memberBytes = checkinId.toString().getBytes(StandardCharsets.UTF_8);

        try {
            redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                for (String key : targetKeys) {
                    byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
                    connection.zSetCommands().zAdd(keyBytes, score, memberBytes);
                    connection.zSetCommands().zRemRange(keyBytes, 0, -(maxTimelineSize + 1L));
                    connection.keyCommands().expire(keyBytes, timelineTtlSeconds);
                }
                return null;
            });
        } catch (Exception e) {
            recordFallback("addCheckinToTimelines", e);
        }
    }

    @Override
    public void removeCheckinFromTimelines(UUID checkinId, UUID communityId, Collection<UUID> userIds) {
        if (!isAvailable() || checkinId == null) {
            return;
        }

        List<String> targetKeys = buildTargetKeys(communityId, userIds);
        if (targetKeys.isEmpty()) {
            return;
        }

        byte[] memberBytes = checkinId.toString().getBytes(StandardCharsets.UTF_8);

        try {
            redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                for (String key : targetKeys) {
                    byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
                    connection.zSetCommands().zRem(keyBytes, memberBytes);
                }
                return null;
            });
        } catch (Exception e) {
            recordFallback("removeCheckinFromTimelines", e);
        }
    }

    @Override
    public FeedTimelineSlice getUserTimeline(UUID userId, Instant cursorTime, UUID cursorId, int limit) {
        if (userId == null) {
            return FeedTimelineSlice.miss();
        }
        return getTimeline(USER_TIMELINE_PREFIX + userId, cursorTime, cursorId, limit);
    }

    @Override
    public FeedTimelineSlice getCommunityTimeline(UUID communityId, Instant cursorTime, UUID cursorId, int limit) {
        if (communityId == null) {
            return FeedTimelineSlice.miss();
        }
        return getTimeline(COMMUNITY_TIMELINE_PREFIX + communityId, cursorTime, cursorId, limit);
    }

    @Override
    public void warmUserTimeline(UUID userId, List<FeedTimelineEntry> entries) {
        if (userId != null) {
            warmTimeline(USER_TIMELINE_PREFIX + userId, entries);
        }
    }

    @Override
    public void warmCommunityTimeline(UUID communityId, List<FeedTimelineEntry> entries) {
        if (communityId != null) {
            warmTimeline(COMMUNITY_TIMELINE_PREFIX + communityId, entries);
        }
    }

    @Override
    public void invalidateUserTimeline(UUID userId) {
        if (!isAvailable() || userId == null) {
            return;
        }
        try {
            redisTemplate.delete(USER_TIMELINE_PREFIX + userId);
        } catch (Exception e) {
            recordFallback("invalidateUserTimeline", e);
        }
    }

    @Override
    public void invalidateCommunityTimeline(UUID communityId) {
        if (!isAvailable() || communityId == null) {
            return;
        }
        try {
            redisTemplate.delete(COMMUNITY_TIMELINE_PREFIX + communityId);
        } catch (Exception e) {
            recordFallback("invalidateCommunityTimeline", e);
        }
    }

    private FeedTimelineSlice getTimeline(String key, Instant cursorTime, UUID cursorId, int limit) {
        if (!isAvailable() || limit <= 0) {
            return FeedTimelineSlice.miss();
        }

        try {
            Boolean exists = redisTemplate.hasKey(key);
            if (!Boolean.TRUE.equals(exists)) {
                return FeedTimelineSlice.miss();
            }

            ZSetOperations<String, String> zSetOps = redisTemplate.opsForZSet();

            if (cursorTime == null) {
                Set<String> raw = zSetOps.reverseRange(key, 0, limit - 1L);
                return FeedTimelineSlice.hit(parseUuids(raw));
            }

            if (cursorId != null) {
                Long rank = zSetOps.reverseRank(key, cursorId.toString());
                if (rank != null) {
                    Set<String> raw = zSetOps.reverseRange(key, rank + 1, rank + limit);
                    List<UUID> ids = parseUuids(raw);
                    if (ids.isEmpty() && isTimelineAtCapacity(zSetOps, key)) {
                        return FeedTimelineSlice.miss();
                    }
                    return FeedTimelineSlice.hit(ids);
                }
            }

            double maxScore = cursorTime.toEpochMilli();
            Set<ZSetOperations.TypedTuple<String>> tuples = zSetOps.reverseRangeByScoreWithScores(
                    key,
                    Double.NEGATIVE_INFINITY,
                    maxScore,
                    0,
                    limit + 10L
            );

            List<UUID> filtered = new ArrayList<>();
            if (tuples != null) {
                String cursorIdStr = cursorId != null ? cursorId.toString() : null;
                for (ZSetOperations.TypedTuple<String> tuple : tuples) {
                    String member = tuple.getValue();
                    Double score = tuple.getScore();
                    if (member == null || score == null) {
                        continue;
                    }
                    long scoreMillis = score.longValue();
                    long cursorMillis = cursorTime.toEpochMilli();
                    boolean isStrictlyOlder = scoreMillis < cursorMillis
                            || (scoreMillis == cursorMillis && cursorIdStr != null && member.compareTo(cursorIdStr) < 0);
                    if (isStrictlyOlder) {
                        try {
                            filtered.add(UUID.fromString(member));
                            if (filtered.size() >= limit) {
                                break;
                            }
                        } catch (IllegalArgumentException ignored) {
                            // Ignore malformed member
                        }
                    }
                }
            }

            if (filtered.isEmpty() && isTimelineAtCapacity(zSetOps, key)) {
                return FeedTimelineSlice.miss();
            }

            return FeedTimelineSlice.hit(filtered);
        } catch (Exception e) {
            recordFallback("getTimeline", e);
            return FeedTimelineSlice.miss();
        }
    }

    private void warmTimeline(String key, List<FeedTimelineEntry> entries) {
        if (!isAvailable() || entries == null || entries.isEmpty()) {
            return;
        }
        try {
            redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
                for (FeedTimelineEntry entry : entries) {
                    if (entry.checkinId() != null && entry.createdAt() != null) {
                        byte[] memberBytes = entry.checkinId().toString().getBytes(StandardCharsets.UTF_8);
                        connection.zSetCommands().zAdd(keyBytes, entry.createdAt().toEpochMilli(), memberBytes);
                    }
                }
                connection.zSetCommands().zRemRange(keyBytes, 0, -(maxTimelineSize + 1L));
                connection.keyCommands().expire(keyBytes, timelineTtlSeconds);
                return null;
            });
        } catch (Exception e) {
            recordFallback("warmTimeline", e);
        }
    }

    private boolean isTimelineAtCapacity(ZSetOperations<String, String> zSetOps, String key) {
        Long size = zSetOps.zCard(key);
        return size != null && size >= maxTimelineSize;
    }

    private List<String> buildTargetKeys(UUID communityId, Collection<UUID> userIds) {
        Set<String> keys = new LinkedHashSet<>();
        if (communityId != null) {
            keys.add(COMMUNITY_TIMELINE_PREFIX + communityId);
        }
        if (userIds != null) {
            for (UUID userId : userIds) {
                if (userId != null) {
                    keys.add(USER_TIMELINE_PREFIX + userId);
                }
            }
        }
        return new ArrayList<>(keys);
    }

    private List<UUID> parseUuids(Set<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        List<UUID> result = new ArrayList<>(raw.size());
        for (String value : raw) {
            try {
                result.add(UUID.fromString(value));
            } catch (IllegalArgumentException ignored) {
                // Ignore malformed UUID
            }
        }
        return result;
    }

    private boolean isAvailable() {
        return redisEnabled && redisTemplate != null;
    }

    private void recordFallback(String operation, Exception e) {
        if (fallbackCounter != null) {
            fallbackCounter.increment();
        }
        log.warn("Redis feed operation '{}' failed, falling back to PostgreSQL: {}", operation, e.getMessage());
    }
}
