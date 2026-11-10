package com.studywithus.gamification.domain;

import java.time.Instant;
import java.util.UUID;

public record ChallengeCompletedEvent(
        UUID challengeId,
        UUID userId,
        UUID badgeId,
        Instant occurredAt
) {
    public ChallengeCompletedEvent(UUID challengeId, UUID userId, UUID badgeId) {
        this(challengeId, userId, badgeId, Instant.now());
    }
}
