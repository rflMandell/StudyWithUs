package com.studywithus.moderation.application;

import com.studywithus.moderation.domain.ReportTargetType;

import java.util.UUID;

public record CreateReportCommand(
        UUID reporterId,
        ReportTargetType targetType,
        UUID targetId,
        String reason,
        String description
) {
}
