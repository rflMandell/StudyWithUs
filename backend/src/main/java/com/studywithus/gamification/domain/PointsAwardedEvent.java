package com.studywithus.gamification.domain;

import java.time.Instant;
import java.util.UUID;

public record PointsAwardedEvent(
        UUID transactionId,
        UUID userId,
        UUID communityId,
        int amount,
        PointTransactionType type,
        Instant occurredAt
) {
    public PointsAwardedEvent(UUID transactionId, UUID userId, UUID communityId, int amount, PointTransactionType type) {
        this(transactionId, userId, communityId, amount, type, Instant.now());
    }
}
