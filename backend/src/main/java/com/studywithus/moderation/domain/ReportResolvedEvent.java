package com.studywithus.moderation.domain;

import java.util.UUID;

public record ReportResolvedEvent(
        UUID reportId,
        UUID reviewerId,
        ReportStatus status,
        ModerationAction action,
        ReportTargetType targetType,
        UUID targetId
) {
}
