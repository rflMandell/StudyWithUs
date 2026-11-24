package com.studywithus.admin.application;

public record SystemOverviewResult(
        long totalUsers,
        long activeUsers,
        long suspendedUsers,
        long newUsersLast24h,
        long activeCommunities,
        long checkinsLast24h,
        long activePremiumSubscriptions,
        long pendingReports
) {
}
