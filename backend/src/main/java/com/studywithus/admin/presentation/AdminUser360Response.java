package com.studywithus.admin.presentation;

import com.studywithus.admin.application.AdminUser360Result;
import com.studywithus.identity.domain.GlobalRole;
import com.studywithus.identity.domain.User;
import com.studywithus.identity.domain.UserStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record AdminUser360Response(
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
        boolean totpEnabled,
        String plan,
        String subscriptionStatus,
        Instant subscriptionExpiresAt,
        long lifetimePoints,
        int currentStreak,
        int longestStreak,
        int level,
        long totalXp,
        String leagueDivision,
        List<AdminUser360Result.CommunitySummary> communities,
        List<AdminUser360Result.ReportSummary> reports,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt
) {
    public static AdminUser360Response from(AdminUser360Result result) {
        User u = result.user();
        return new AdminUser360Response(
                u.getId(),
                u.getUsername(),
                u.getName(),
                u.getEmail(),
                u.getBirthDate(),
                u.getAvatarUrl(),
                u.getInstitution(),
                u.getCourse(),
                u.getStudyGoal(),
                u.getBio(),
                u.getStatus(),
                u.getEffectiveGlobalRole(),
                result.totpEnabled(),
                result.plan(),
                result.subscriptionStatus(),
                result.subscriptionExpiresAt(),
                u.getLifetimePoints(),
                result.currentStreak(),
                result.longestStreak(),
                result.level(),
                result.totalXp(),
                result.leagueDivision(),
                result.communities(),
                result.reports(),
                u.getCreatedAt(),
                u.getUpdatedAt(),
                u.getDeletedAt()
        );
    }
}
