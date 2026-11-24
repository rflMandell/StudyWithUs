package com.studywithus.admin.presentation;

import jakarta.validation.constraints.NotBlank;

public record AdminAccountActionRequest(
        @NotBlank(message = "Reason is required")
        String reason
) {
}
