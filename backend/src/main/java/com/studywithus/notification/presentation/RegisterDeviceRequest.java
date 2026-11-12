package com.studywithus.notification.presentation;

import com.studywithus.notification.application.RegisterDeviceTokenCommand;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record RegisterDeviceRequest(
        @NotBlank(message = "O token do dispositivo é obrigatório")
        String token,

        String platform
) {
    public RegisterDeviceTokenCommand toCommand(UUID userId) {
        return new RegisterDeviceTokenCommand(userId, token, platform);
    }
}
