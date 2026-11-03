package com.studywithus.gamification.application;

import com.studywithus.gamification.domain.UserBadge;

import java.time.Instant;
import java.util.UUID;

public record UserBadgeResult(
        UUID badgeId,
        String name,
        String description,
        String imageUrl,
        Instant awardedAt
) {
    public static UserBadgeResult from(UserBadge ub) {
        return new UserBadgeResult(
                ub.getBadgeId(),
                ub.getName(),
                ub.getDescription(),
                ub.getImageUrl(),
                ub.getAwardedAt()
        );
    }
}
