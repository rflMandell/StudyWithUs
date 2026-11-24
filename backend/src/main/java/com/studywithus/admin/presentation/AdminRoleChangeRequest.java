package com.studywithus.admin.presentation;

import com.studywithus.identity.domain.GlobalRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdminRoleChangeRequest(
        @NotNull(message = "globalRole is required")
        GlobalRole globalRole,

        @NotBlank(message = "Reason is required")
        String reason
) {
}
