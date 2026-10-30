package com.studywithus.identity.application;

import java.util.UUID;

public record AuthResult(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UUID userId,
        String username,
        String email,
        String name
) {
    public static AuthResult bearer(String accessToken, String refreshToken, long expiresIn,
                                    UUID userId, String username, String email, String name) {
        return new AuthResult(accessToken, refreshToken, "Bearer", expiresIn, userId, username, email, name);
    }
}
