package com.studywithus.identity.application;

import com.studywithus.gamification.domain.UserBadge;
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
        UserStatus status,
        long currentPoints,
        long lifetimePoints,
        int activeCommunitiesCount,
        long checkinsCount,
        int currentStreak,
        int longestStreak,
        List<UserBadge> badges,
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
                user.getStatus(),
                currentPoints,
                user.getLifetimePoints(),
                activeCommunitiesCount,
                checkinsCount,
                currentStreak,
                longestStreak,
                badges != null ? badges : Collections.emptyList(),
                user.getCreatedAt()
        );
    }
}
