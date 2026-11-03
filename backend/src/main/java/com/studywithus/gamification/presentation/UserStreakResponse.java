package com.studywithus.gamification.presentation;

import com.studywithus.gamification.application.UserStreakResult;

import java.time.LocalDate;
import java.util.UUID;

public record UserStreakResponse(
        UUID userId,
        int currentStreak,
        int longestStreak,
        LocalDate lastActivityDate
) {
    public static UserStreakResponse from(UserStreakResult result) {
        return new UserStreakResponse(
                result.userId(),
                result.currentStreak(),
                result.longestStreak(),
                result.lastActivityDate()
        );
    }
}
