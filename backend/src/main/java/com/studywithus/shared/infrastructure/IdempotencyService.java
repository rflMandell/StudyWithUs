package com.studywithus.shared.infrastructure;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class IdempotencyService {

    private record Entry(Object value, Instant expiresAt) {}

    private final Map<String, Entry> cache = new ConcurrentHashMap<>();

    public <T> Optional<T> get(String key, Class<T> clazz) {
        Entry entry = cache.get(key);
        if (entry == null) {
            return Optional.empty();
        }
        if (Instant.now().isAfter(entry.expiresAt())) {
            cache.remove(key);
            return Optional.empty();
        }
        return Optional.ofNullable(clazz.cast(entry.value()));
    }

    public void set(String key, Object value, long ttlSeconds) {
        cache.put(key, new Entry(value, Instant.now().plusSeconds(ttlSeconds)));
    }
}
