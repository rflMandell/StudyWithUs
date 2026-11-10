package com.studywithus.gamification.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BadgeRepository {
    Badge saveBadge(Badge badge);
    Optional<Badge> findById(UUID id);
    Optional<Badge> findByName(String name);
    List<Badge> findAllBadges();
    List<UserBadge> findUserBadges(UUID userId);
    boolean hasBadge(UUID userId, UUID badgeId);
    void assignBadgeToUser(UUID userId, UUID badgeId, Instant awardedAt, UUID challengeId, UUID communityId);
}
