package com.studywithus.identity.domain;

import java.time.Instant;
import java.util.UUID;

public record UserRegisteredEvent(
        UUID userId,
        String username,
        String email,
        Instant occurredAt
) {
    public static UserRegisteredEvent of(User user) {
        return new UserRegisteredEvent(user.getId(), user.getUsername(), user.getEmail(), Instant.now());
    }
}
