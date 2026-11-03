package com.studywithus.gamification.presentation;

import com.studywithus.gamification.application.GetCommunityStreakUseCase.CommunityStreakResult;

import java.time.LocalDate;
import java.util.UUID;

public record CommunityStreakResponse(
        UUID communityId,
        UUID userId,
        int currentStreak,
        int longestStreak,
        LocalDate lastActivityDate
) {
    public static CommunityStreakResponse from(CommunityStreakResult result) {
        return new CommunityStreakResponse(
                result.communityId(),
                result.userId(),
                result.currentStreak(),
                result.longestStreak(),
                result.lastActivityDate()
        );
    }
}
