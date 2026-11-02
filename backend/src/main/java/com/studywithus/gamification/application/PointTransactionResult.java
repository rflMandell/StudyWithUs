package com.studywithus.gamification.application;

import com.studywithus.gamification.domain.PointTransaction;
import com.studywithus.gamification.domain.PointTransactionType;

import java.time.Instant;
import java.util.UUID;

public record PointTransactionResult(
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
    public static PointTransactionResult fromDomain(PointTransaction domain) {
        return new PointTransactionResult(
                domain.getId(),
                domain.getUserId(),
                domain.getCommunityId(),
                domain.getCheckinId(),
                domain.getChallengeId(),
                domain.getAmount(),
                domain.getType(),
                domain.getCreatedAt(),
                domain.getReversedAt(),
                domain.getReversalId()
        );
    }
}
