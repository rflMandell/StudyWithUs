package com.studywithus.moderation.domain;

import java.util.UUID;

public record ReportCreatedEvent(
        UUID reportId,
        UUID reporterId,
        ReportTargetType targetType,
        UUID targetId,
        String reason
) {
}
