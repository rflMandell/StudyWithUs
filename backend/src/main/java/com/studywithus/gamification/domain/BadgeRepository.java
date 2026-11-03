package com.studywithus.gamification.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BadgeRepository {
    Badge saveBadge(Badge badge);
    Optional<Badge> findById(UUID id);
    List<UserBadge> findUserBadges(UUID userId);
    void assignBadgeToUser(UUID userId, UUID badgeId, Instant awardedAt, UUID challengeId, UUID communityId);
}
