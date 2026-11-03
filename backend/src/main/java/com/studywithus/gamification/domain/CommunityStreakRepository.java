package com.studywithus.gamification.domain;

import java.util.Optional;
import java.util.UUID;

public interface CommunityStreakRepository {
    CommunityStreak save(CommunityStreak streak);
    Optional<CommunityStreak> findByCommunityIdAndUserId(UUID communityId, UUID userId);
}
