package com.studywithus.gamification.domain;

import java.time.Instant;
import java.util.UUID;

public record UserLevelUpEvent(
        UUID userId,
        int previousLevel,
        int newLevel,
        long totalXp,
        String unlockedTitle,
        String unlockedAvatarBorder,
        Instant occurredAt
) {
    public UserLevelUpEvent(
            UUID userId,
            int previousLevel,
            int newLevel,
            long totalXp,
            String unlockedTitle,
            String unlockedAvatarBorder
    ) {
        this(userId, previousLevel, newLevel, totalXp, unlockedTitle, unlockedAvatarBorder, Instant.now());
    }
}
