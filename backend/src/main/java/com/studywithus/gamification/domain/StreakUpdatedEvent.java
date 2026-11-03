package com.studywithus.gamification.domain;

import java.time.LocalDate;
import java.util.UUID;

public record StreakUpdatedEvent(
        UUID userId,
        UUID communityId,
        int userCurrentStreak,
        int communityCurrentStreak,
        LocalDate activityDate
) {
}
