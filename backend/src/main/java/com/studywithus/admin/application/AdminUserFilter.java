package com.studywithus.admin.application;

import com.studywithus.identity.domain.GlobalRole;
import com.studywithus.identity.domain.UserStatus;

import java.time.Instant;

public record AdminUserFilter(
        UserStatus status,
        GlobalRole globalRole,
        String plan,
        Instant createdFrom,
        Instant createdTo,
        String search
) {
    public static AdminUserFilter empty() {
        return new AdminUserFilter(null, null, null, null, null, null);
    }
}
