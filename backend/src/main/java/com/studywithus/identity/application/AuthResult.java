package com.studywithus.identity.application;

import com.studywithus.identity.domain.GlobalRole;

import java.util.UUID;

public record AuthResult(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UUID userId,
        String username,
        String email,
        String name,
        GlobalRole globalRole
) {
    public static AuthResult bearer(String accessToken, String refreshToken, long expiresIn,
                                    UUID userId, String username, String email, String name) {
        return new AuthResult(accessToken, refreshToken, "Bearer", expiresIn, userId, username, email, name, GlobalRole.USER);
    }

    public static AuthResult bearer(String accessToken, String refreshToken, long expiresIn,
                                    UUID userId, String username, String email, String name, GlobalRole globalRole) {
        return new AuthResult(
                accessToken,
                refreshToken,
                "Bearer",
                expiresIn,
                userId,
                username,
                email,
                name,
                globalRole != null ? globalRole : GlobalRole.USER
        );
    }
}
