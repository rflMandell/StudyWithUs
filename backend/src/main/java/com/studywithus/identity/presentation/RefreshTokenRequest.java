package com.studywithus.identity.presentation;

import com.studywithus.identity.application.RefreshTokenCommand;
import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
        @NotBlank(message = "Refresh token is required")
        String refreshToken,

        String deviceId
) {
    public RefreshTokenCommand toCommand() {
        return new RefreshTokenCommand(refreshToken, deviceId);
    }
}
