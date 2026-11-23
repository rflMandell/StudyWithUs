package com.studywithus.gamification.application;

import com.studywithus.gamification.domain.XpSourceType;

import java.util.UUID;

public record AwardXpCommand(
        UUID userId,
        XpSourceType sourceType,
        String sourceId,
        long amount
) {
}
