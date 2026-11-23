package com.studywithus.league.infrastructure;

import com.studywithus.league.domain.LeagueLeaderboardCachePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
public class RedisLeagueLeaderboardAdapter implements LeagueLeaderboardCachePort {

    private static final String POOL_KEY_PREFIX = "league:pool:";
    private static final long TTL_SECONDS = Duration.ofDays(14).toSeconds();

    private final StringRedisTemplate redisTemplate;
    private final boolean redisEnabled;

    public RedisLeagueLeaderboardAdapter(
            @Autowired(required = false) StringRedisTemplate redisTemplate,
            @Value("${app.league.redis-enabled:true}") boolean redisEnabled
    ) {
        this.redisTemplate = redisTemplate;
        this.redisEnabled = redisEnabled;
    }

    @Override
    public void upsertScore(UUID poolId, UUID userId, long weeklyScore) {
        if (!isAvailable() || poolId == null || userId == null) {
            return;
        }
        String key = POOL_KEY_PREFIX + poolId;
        try {
            redisTemplate.opsForZSet().add(key, userId.toString(), (double) weeklyScore);
            redisTemplate.expire(key, Duration.ofSeconds(TTL_SECONDS));
        } catch (Exception e) {
            log.warn("Redis league upsertScore failed for poolId={}: {}", poolId, e.getMessage());
        }
    }

    @Override
    public Optional<List<LeaderboardScoreEntry>> getPoolLeaderboard(UUID poolId) {
        if (!isAvailable() || poolId == null) {
            return Optional.empty();
        }
        String key = POOL_KEY_PREFIX + poolId;
        try {
            if (!Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
                return Optional.empty();
            }
            Set<ZSetOperations.TypedTuple<String>> tuples =
                    redisTemplate.opsForZSet().reverseRangeWithScores(key, 0, -1);
            if (tuples == null) {
                return Optional.empty();
            }
            List<LeaderboardScoreEntry> entries = new ArrayList<>(tuples.size());
            for (ZSetOperations.TypedTuple<String> tuple : tuples) {
                if (tuple.getValue() != null && tuple.getScore() != null) {
                    entries.add(new LeaderboardScoreEntry(
                            UUID.fromString(tuple.getValue()),
                            tuple.getScore().longValue()
                    ));
                }
            }
            return Optional.of(entries);
        } catch (Exception e) {
            log.warn("Redis league getPoolLeaderboard failed for poolId={}, falling back to PostgreSQL: {}", poolId, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void warmPoolLeaderboard(UUID poolId, List<LeaderboardScoreEntry> entries) {
        if (!isAvailable() || poolId == null || entries == null || entries.isEmpty()) {
            return;
        }
        String key = POOL_KEY_PREFIX + poolId;
        try {
            redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
                for (LeaderboardScoreEntry entry : entries) {
                    byte[] memberBytes = entry.userId().toString().getBytes(StandardCharsets.UTF_8);
                    connection.zSetCommands().zAdd(keyBytes, entry.score(), memberBytes);
                }
                connection.keyCommands().expire(keyBytes, TTL_SECONDS);
                return null;
            });
        } catch (Exception e) {
            log.warn("Redis league warmPoolLeaderboard failed for poolId={}: {}", poolId, e.getMessage());
        }
    }

    @Override
    public void invalidatePool(UUID poolId) {
        if (!isAvailable() || poolId == null) {
            return;
        }
        try {
            redisTemplate.delete(POOL_KEY_PREFIX + poolId);
        } catch (Exception e) {
            log.warn("Redis league invalidatePool failed for poolId={}: {}", poolId, e.getMessage());
        }
    }

    private boolean isAvailable() {
        return redisEnabled && redisTemplate != null;
    }
}
