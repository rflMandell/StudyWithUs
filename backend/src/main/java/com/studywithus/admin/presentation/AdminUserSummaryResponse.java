package com.studywithus.admin.presentation;

import com.studywithus.admin.application.AdminUserListItem;
import com.studywithus.identity.domain.GlobalRole;
import com.studywithus.identity.domain.User;
import com.studywithus.identity.domain.UserStatus;

import java.time.Instant;
import java.util.UUID;

public record AdminUserSummaryResponse(
        UUID id,
        String username,
        String name,
        String email,
        UserStatus status,
        GlobalRole globalRole,
        String plan,
        long lifetimePoints,
        Instant createdAt
) {
    public static AdminUserSummaryResponse from(AdminUserListItem item) {
        User user = item.user();
        return new AdminUserSummaryResponse(
                user.getId(),
                user.getUsername(),
                user.getName(),
                user.getEmail(),
                user.getStatus(),
                user.getEffectiveGlobalRole(),
                item.plan(),
                user.getLifetimePoints(),
                user.getCreatedAt()
        );
    }

    public static AdminUserSummaryResponse from(User user) {
        return new AdminUserSummaryResponse(
                user.getId(),
                user.getUsername(),
                user.getName(),
                user.getEmail(),
                user.getStatus(),
                user.getEffectiveGlobalRole(),
                "FREE",
                user.getLifetimePoints(),
                user.getCreatedAt()
        );
    }
}
