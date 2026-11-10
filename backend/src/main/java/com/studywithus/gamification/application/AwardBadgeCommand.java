package com.studywithus.gamification.application;

import java.util.UUID;

public record AwardBadgeCommand(
        UUID userId,
        UUID badgeId,
        UUID challengeId,
        UUID communityId
) {
    public AwardBadgeCommand(UUID userId, UUID badgeId) {
        this(userId, badgeId, null, null);
    }

    public AwardBadgeCommand(UUID userId, UUID badgeId, UUID communityId) {
        this(userId, badgeId, null, communityId);
    }
}
