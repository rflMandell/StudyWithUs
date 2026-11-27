package com.studywithus.identity.application;

import com.studywithus.gamification.domain.UserBadge;
import com.studywithus.gamification.domain.UserLevel;
import com.studywithus.identity.domain.GlobalRole;
import com.studywithus.identity.domain.User;
import com.studywithus.identity.domain.UserStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public record UserProfileResult(
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
        boolean isPrivate,
        Instant privacyUpdatedAt,
        UserStatus status,
        GlobalRole globalRole,
        long currentPoints,
        long lifetimePoints,
        int activeCommunitiesCount,
        long checkinsCount,
        int currentStreak,
        int longestStreak,
        List<UserBadge> badges,
        int level,
        long totalXp,
        long currentLevelXp,
        long nextLevelXp,
        double progressPercent,
        String activeTitle,
        String activeAvatarBorder,
        Instant createdAt
) {
    public static UserProfileResult from(
            User user,
            long currentPoints,
            int activeCommunitiesCount,
            long checkinsCount,
            int currentStreak,
            int longestStreak,
            List<UserBadge> badges
    ) {
        return from(
                user,
                currentPoints,
                activeCommunitiesCount,
                checkinsCount,
                currentStreak,
                longestStreak,
                badges,
                UserLevel.initial(user.getId())
        );
    }

    public static UserProfileResult from(
            User user,
            long currentPoints,
            int activeCommunitiesCount,
            long checkinsCount,
            int currentStreak,
            int longestStreak,
            List<UserBadge> badges,
            UserLevel userLevel
    ) {
        UserLevel safeLevel = userLevel != null ? userLevel : UserLevel.initial(user.getId());
        return new UserProfileResult(
                user.getId(),
                user.getUsername(),
                user.getName(),
                user.getEmail(),
                user.getBirthDate(),
                user.getAvatarUrl(),
                user.getInstitution(),
                user.getCourse(),
                user.getStudyGoal(),
                user.getBio(),
                user.isPrivate(),
                user.getPrivacyUpdatedAt(),
                user.getStatus(),
                user.getEffectiveGlobalRole(),
                currentPoints,
                user.getLifetimePoints(),
                activeCommunitiesCount,
                checkinsCount,
                currentStreak,
                longestStreak,
                badges != null ? badges : Collections.emptyList(),
                safeLevel.getCurrentLevel(),
                safeLevel.getTotalXp(),
                safeLevel.getCurrentLevelXp(),
                safeLevel.getNextLevelXp(),
                safeLevel.progressPercent(),
                safeLevel.getActiveTitle(),
                safeLevel.getActiveAvatarBorder(),
                user.getCreatedAt()
        );
    }
}
