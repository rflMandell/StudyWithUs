package com.studywithus.shared.infrastructure;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitingServiceTest {

    private final RateLimitingService rateLimitingService = new RateLimitingService(null);

    @Test
    @DisplayName("Should allow requests within limit and reject when limit is exceeded")
    void shouldRateLimitCorrectly() {
        String key = "test-client-ip";
        int limit = 3;
        int window = 10;

        assertThat(rateLimitingService.tryAcquire(key, limit, window)).isTrue();
        assertThat(rateLimitingService.tryAcquire(key, limit, window)).isTrue();
        assertThat(rateLimitingService.tryAcquire(key, limit, window)).isTrue();

        // 4th request exceeds the limit of 3
        assertThat(rateLimitingService.tryAcquire(key, limit, window)).isFalse();

        // After reset, request is allowed again
        rateLimitingService.reset(key);
        assertThat(rateLimitingService.tryAcquire(key, limit, window)).isTrue();
    }
}
