package com.studywithus.gamification.presentation;

import com.studywithus.gamification.domain.Badge;

import java.time.Instant;
import java.util.UUID;

public record BadgeResponse(
        UUID id,
        String name,
        String description,
        String imageUrl,
        Instant createdAt
) {
    public static BadgeResponse from(Badge badge) {
        return new BadgeResponse(
                badge.getId(),
                badge.getName(),
                badge.getDescription(),
                badge.getImageUrl(),
                badge.getCreatedAt()
        );
    }
}
