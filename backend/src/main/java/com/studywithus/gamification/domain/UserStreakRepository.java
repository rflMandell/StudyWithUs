package com.studywithus.gamification.domain;

import java.util.Optional;
import java.util.UUID;

public interface UserStreakRepository {
    UserStreak save(UserStreak streak);
    Optional<UserStreak> findByUserId(UUID userId);
}
