package com.studywithus.moderation.application;

import java.util.UUID;

public record DismissReportCommand(
        UUID reportId,
        UUID reviewerId,
        String notes
) {
}
