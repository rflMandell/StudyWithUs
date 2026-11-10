package com.studywithus.gamification.domain;

import java.time.Instant;
import java.util.UUID;

public record BadgeGrantedEvent(
        UUID userId,
        UUID badgeId,
        String badgeName,
        UUID communityId,
        Instant occurredAt
) {
    public BadgeGrantedEvent(UUID userId, UUID badgeId, String badgeName, UUID communityId) {
        this(userId, badgeId, badgeName, communityId, Instant.now());
    }
}
