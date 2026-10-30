package com.studywithus.identity.presentation;

import com.studywithus.identity.application.AuthResult;

import java.util.UUID;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UUID userId,
        String username,
        String email,
        String name
) {
    public static AuthResponse from(AuthResult result) {
        return new AuthResponse(
                result.accessToken(),
                result.refreshToken(),
                result.tokenType(),
                result.expiresIn(),
                result.userId(),
                result.username(),
                result.email(),
                result.name()
        );
    }
}
