package com.studywithus.admin.application;

import com.studywithus.identity.domain.GlobalRole;

import java.util.UUID;

public record AdminSessionResult(
        UUID userId,
        String username,
        String email,
        String name,
        GlobalRole globalRole,
        boolean totpEnabled,
        boolean totpEligible,
        long accessTokenTtlSeconds
) {
}
