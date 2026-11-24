package com.studywithus.identity.presentation;

import com.studywithus.identity.application.LoginCommand;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Username or email is required")
        String usernameOrEmail,

        @NotBlank(message = "Password is required")
        String password,

        String deviceId,

        String totpCode
) {
    public LoginRequest(String usernameOrEmail, String password, String deviceId) {
        this(usernameOrEmail, password, deviceId, null);
    }

    public LoginCommand toCommand() {
        return new LoginCommand(usernameOrEmail, password, deviceId, totpCode);
    }
}
