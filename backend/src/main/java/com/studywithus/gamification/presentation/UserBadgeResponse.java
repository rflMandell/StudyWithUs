package com.studywithus.gamification.presentation;

import com.studywithus.gamification.application.UserBadgeResult;

import java.time.Instant;
import java.util.UUID;

public record UserBadgeResponse(
        UUID badgeId,
        String name,
        String description,
        String imageUrl,
        Instant awardedAt
) {
    public static UserBadgeResponse from(UserBadgeResult result) {
        return new UserBadgeResponse(
                result.badgeId(),
                result.name(),
                result.description(),
                result.imageUrl(),
                result.awardedAt()
        );
    }
}
