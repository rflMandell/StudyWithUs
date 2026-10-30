package com.studywithus.identity.application;

public record RefreshTokenCommand(
        String refreshToken,
        String deviceId
) {
}
