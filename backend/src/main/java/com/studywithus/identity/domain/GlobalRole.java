package com.studywithus.identity.domain;

/**
 * Global platform-level RBAC roles, strictly isolated from contextual community roles
 * (OWNER, ADMIN, MODERATOR, MEMBER).
 */
public enum GlobalRole {
    USER,
    SUPPORT,
    ADMIN,
    SUPER_ADMIN;

    public boolean isAdminAccess() {
        return this == SUPPORT || this == ADMIN || this == SUPER_ADMIN;
    }

    public boolean requiresTwoFactorEligibility() {
        return this == ADMIN || this == SUPER_ADMIN;
    }
}
