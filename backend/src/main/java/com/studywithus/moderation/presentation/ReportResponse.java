package com.studywithus.moderation.presentation;

import com.studywithus.moderation.application.ReportResult;
import com.studywithus.moderation.domain.ReportStatus;
import com.studywithus.moderation.domain.ReportTargetType;

import java.time.Instant;
import java.util.UUID;

public record ReportResponse(
        UUID id,
        UUID reporterId,
        ReportTargetType targetType,
        UUID targetId,
        String reason,
        String description,
        ReportStatus status,
        UUID reviewedBy,
        Instant reviewedAt,
        Instant createdAt
) {
    public static ReportResponse from(ReportResult result) {
        return new ReportResponse(
                result.id(),
                result.reporterId(),
                result.targetType(),
                result.targetId(),
                result.reason(),
                result.description(),
                result.status(),
                result.reviewedBy(),
                result.reviewedAt(),
                result.createdAt()
        );
    }
}
