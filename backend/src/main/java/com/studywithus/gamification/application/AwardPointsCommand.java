package com.studywithus.gamification.application;

import com.studywithus.gamification.domain.PointTransactionType;

import java.util.UUID;

public record AwardPointsCommand(
        UUID userId,
        UUID communityId,
        UUID checkinId,
        UUID challengeId,
        int amount,
        PointTransactionType type
) {
}
