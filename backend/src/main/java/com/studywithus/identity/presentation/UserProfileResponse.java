package com.studywithus.identity.presentation;

import com.studywithus.gamification.presentation.UserBadgeResponse;
import com.studywithus.identity.application.UserProfileResult;
import com.studywithus.identity.domain.GlobalRole;
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
        GlobalRole globalRole,
        long currentPoints,
        long lifetimePoints,
        int activeCommunitiesCount,
        long checkinsCount,
        int currentStreak,
        int longestStreak,
        List<UserBadgeResponse> badges,
        int level,
        long totalXp,
        long currentLevelXp,
        long nextLevelXp,
        double progressPercent,
        String activeTitle,
        String activeAvatarBorder,
        Instant createdAt
) {
    public static UserProfileResponse from(UserProfileResult result) {
        return from(result, true);
    }

    public static UserProfileResponse fromPublic(UserProfileResult result) {
        return from(result, false);
    }

    private static UserProfileResponse from(UserProfileResult result, boolean includePrivateFields) {
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
                includePrivateFields ? result.email() : null,
                includePrivateFields ? result.birthDate() : null,
                result.avatarUrl(),
                result.institution(),
                result.course(),
                result.studyGoal(),
                result.bio(),
                result.status(),
                includePrivateFields ? result.globalRole() : null,
                result.currentPoints(),
                result.lifetimePoints(),
                result.activeCommunitiesCount(),
                result.checkinsCount(),
                result.currentStreak(),
                result.longestStreak(),
                badgeResponses,
                result.level(),
                result.totalXp(),
                result.currentLevelXp(),
                result.nextLevelXp(),
                result.progressPercent(),
                result.activeTitle(),
                result.activeAvatarBorder(),
                result.createdAt()
        );
    }
}
