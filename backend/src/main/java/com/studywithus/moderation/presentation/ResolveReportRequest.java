package com.studywithus.moderation.presentation;

import com.studywithus.moderation.domain.ModerationAction;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ResolveReportRequest(
        @NotNull(message = "Action is required")
        ModerationAction action,

        @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
        String notes
) {
}
