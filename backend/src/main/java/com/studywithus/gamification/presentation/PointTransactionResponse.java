package com.studywithus.gamification.presentation;

import com.studywithus.gamification.application.PointTransactionResult;
import com.studywithus.gamification.domain.PointTransactionType;

import java.time.Instant;
import java.util.UUID;

public record PointTransactionResponse(
        UUID id,
        UUID userId,
        UUID communityId,
        UUID checkinId,
        UUID challengeId,
        int amount,
        PointTransactionType type,
        Instant createdAt,
        Instant reversedAt,
        UUID reversalId
) {
    public static PointTransactionResponse from(PointTransactionResult result) {
        return new PointTransactionResponse(
                result.id(),
                result.userId(),
                result.communityId(),
                result.checkinId(),
                result.challengeId(),
                result.amount(),
                result.type(),
                result.createdAt(),
                result.reversedAt(),
                result.reversalId()
        );
    }
}
