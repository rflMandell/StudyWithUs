package com.studywithus.moderation.application;

import com.studywithus.moderation.domain.Report;
import com.studywithus.moderation.domain.ReportStatus;
import com.studywithus.moderation.domain.ReportTargetType;

import java.time.Instant;
import java.util.UUID;

public record ReportResult(
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
    public static ReportResult fromDomain(Report report) {
        return new ReportResult(
                report.getId(),
                report.getReporterId(),
                report.getTargetType(),
                report.getTargetId(),
                report.getReason(),
                report.getDescription(),
                report.getStatus(),
                report.getReviewedBy(),
                report.getReviewedAt(),
                report.getCreatedAt()
        );
    }
}
