package com.studywithus.admin.application;

import com.studywithus.identity.domain.User;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AdminUser360Result(
        User user,
        boolean totpEnabled,
        String plan,
        String subscriptionStatus,
        Instant subscriptionExpiresAt,
        int currentStreak,
        int longestStreak,
        int level,
        long totalXp,
        String leagueDivision,
        List<CommunitySummary> communities,
        List<ReportSummary> reports
) {
    public record CommunitySummary(
            UUID communityId,
            String name,
            String role,
            String status,
            Instant joinedAt
    ) {
    }

    public record ReportSummary(
            UUID id,
            String targetType,
            UUID targetId,
            String reason,
            String status,
            boolean isReporter,
            Instant createdAt
    ) {
    }
}
