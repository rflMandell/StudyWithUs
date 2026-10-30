package com.studywithus.identity.presentation;

import com.studywithus.identity.application.LoginCommand;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Username or email is required")
        String usernameOrEmail,

        @NotBlank(message = "Password is required")
        String password,

        String deviceId
) {
    public LoginCommand toCommand() {
        return new LoginCommand(usernameOrEmail, password, deviceId);
    }
}
