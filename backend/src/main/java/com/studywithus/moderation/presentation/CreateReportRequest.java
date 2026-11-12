package com.studywithus.moderation.presentation;

import com.studywithus.moderation.domain.ReportTargetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateReportRequest(
        @NotNull(message = "Target type is required")
        ReportTargetType targetType,

        @NotNull(message = "Target ID is required")
        UUID targetId,

        @NotBlank(message = "Reason is required")
        @Size(max = 100, message = "Reason cannot exceed 100 characters")
        String reason,

        @Size(max = 1000, message = "Description cannot exceed 1000 characters")
        String description
) {
}
