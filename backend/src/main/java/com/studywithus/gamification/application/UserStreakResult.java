package com.studywithus.gamification.application;

import com.studywithus.gamification.domain.UserStreak;

import java.time.LocalDate;
import java.util.UUID;

public record UserStreakResult(
        UUID userId,
        int currentStreak,
        int longestStreak,
        LocalDate lastActivityDate
) {
    public static UserStreakResult from(UserStreak streak) {
        return new UserStreakResult(
                streak.getUserId(),
                streak.getCurrentStreak(),
                streak.getLongestStreak(),
                streak.getLastActivityDate()
        );
    }
}
