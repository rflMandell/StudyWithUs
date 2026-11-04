package com.studywithus.shared.infrastructure;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class RateLimitingService {

    private final StringRedisTemplate redisTemplate;

    // In-memory fallback if Redis is unavailable or unconfigured
    private final Map<String, WindowCounter> localCounters = new ConcurrentHashMap<>();

    private record WindowCounter(AtomicInteger count, Instant expiresAt) {}

    public RateLimitingService(@Autowired(required = false) StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Checks if a request can proceed under the specified rate limit.
     *
     * @param key unique identifier (e.g. IP + endpoint)
     * @param maxRequests max requests allowed within window
     * @param windowSeconds length of sliding/fixed window in seconds
     * @return true if request allowed, false if limit exceeded
     */
    public boolean tryAcquire(String key, int maxRequests, int windowSeconds) {
        if (redisTemplate != null) {
            try {
                String redisKey = "rate_limit:" + key;
                Long current = redisTemplate.opsForValue().increment(redisKey);
                if (current != null && current == 1) {
                    redisTemplate.expire(redisKey, Duration.ofSeconds(windowSeconds));
                }
                return current != null && current <= maxRequests;
            } catch (Exception e) {
                log.warn("Redis rate limit operation failed, falling back to local counter: {}", e.getMessage());
            }
        }

        // Local in-memory counter fallback
        Instant now = Instant.now();
        WindowCounter counter = localCounters.compute(key, (k, existing) -> {
            if (existing == null || now.isAfter(existing.expiresAt())) {
                return new WindowCounter(new AtomicInteger(1), now.plusSeconds(windowSeconds));
            }
            existing.count().incrementAndGet();
            return existing;
        });

        return counter.count().get() <= maxRequests;
    }

    public void reset(String key) {
        if (redisTemplate != null) {
            try {
                redisTemplate.delete("rate_limit:" + key);
            } catch (Exception ignored) {}
        }
        localCounters.remove(key);
    }
}
