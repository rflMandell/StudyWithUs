package com.studywithus.admin.presentation;

import jakarta.validation.constraints.NotBlank;

public record TwoFactorCodeRequest(
        @NotBlank(message = "TOTP code is required")
        String code
) {
}
