package com.studywithus.gamification.domain;

import java.time.Instant;
import java.util.UUID;

public record PointsReversedEvent(
        UUID reversalTransactionId,
        UUID originalTransactionId,
        UUID userId,
        UUID communityId,
        int amount,
        Instant occurredAt
) {
    public PointsReversedEvent(UUID reversalTransactionId, UUID originalTransactionId, UUID userId, UUID communityId, int amount) {
        this(reversalTransactionId, originalTransactionId, userId, communityId, amount, Instant.now());
    }
}
