package com.studywithus.identity.domain;

import java.time.Instant;
import java.util.UUID;

public record UserLoggedInEvent(
        UUID userId,
        String deviceId,
        Instant occurredAt
) {
    public static UserLoggedInEvent of(UUID userId, String deviceId) {
        return new UserLoggedInEvent(userId, deviceId, Instant.now());
    }
}
