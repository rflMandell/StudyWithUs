package com.studywithus.identity.presentation;

import com.studywithus.gamification.presentation.UserBadgeResponse;
import com.studywithus.identity.application.UserProfileResult;
import com.studywithus.identity.domain.UserStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String username,
        String name,
        String email,
        LocalDate birthDate,
        String avatarUrl,
        String institution,
        String course,
        String studyGoal,
        String bio,
        UserStatus status,
        long currentPoints,
        long lifetimePoints,
        int activeCommunitiesCount,
        long checkinsCount,
        int currentStreak,
        int longestStreak,
        List<UserBadgeResponse> badges,
        Instant createdAt
) {
    public static UserProfileResponse from(UserProfileResult result) {
        List<UserBadgeResponse> badgeResponses = result.badges() != null
                ? result.badges().stream().map(b -> new UserBadgeResponse(
                        b.getBadgeId(),
                        b.getName(),
                        b.getDescription(),
                        b.getImageUrl(),
                        b.getAwardedAt()
                )).toList()
                : List.of();

        return new UserProfileResponse(
                result.id(),
                result.username(),
                result.name(),
                result.email(),
                result.birthDate(),
                result.avatarUrl(),
                result.institution(),
                result.course(),
                result.studyGoal(),
                result.bio(),
                result.status(),
                result.currentPoints(),
                result.lifetimePoints(),
                result.activeCommunitiesCount(),
                result.checkinsCount(),
                result.currentStreak(),
                result.longestStreak(),
                badgeResponses,
                result.createdAt()
        );
    }
}
