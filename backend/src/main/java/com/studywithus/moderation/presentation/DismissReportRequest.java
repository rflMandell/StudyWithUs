package com.studywithus.moderation.presentation;

import jakarta.validation.constraints.Size;

public record DismissReportRequest(
        @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
        String notes
) {
}
