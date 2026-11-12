package com.studywithus.moderation.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Report {
    private UUID id;
    private UUID reporterId;
    private ReportTargetType targetType;
    private UUID targetId;
    private String reason;
    private String description;
    @Builder.Default
    private ReportStatus status = ReportStatus.PENDING;
    private UUID reviewedBy;
    private Instant reviewedAt;
    private Instant createdAt;

    public static Report create(
            UUID reporterId,
            ReportTargetType targetType,
            UUID targetId,
            String reason,
            String description
    ) {
        return Report.builder()
                .id(UUID.randomUUID())
                .reporterId(reporterId)
                .targetType(targetType)
                .targetId(targetId)
                .reason(reason)
                .description(description)
                .status(ReportStatus.PENDING)
                .createdAt(Instant.now())
                .build();
    }

    public boolean isPending() {
        return this.status == ReportStatus.PENDING;
    }

    public void resolve(UUID reviewerId) {
        this.status = ReportStatus.RESOLVED;
        this.reviewedBy = reviewerId;
        this.reviewedAt = Instant.now();
    }

    public void dismiss(UUID reviewerId) {
        this.status = ReportStatus.DISMISSED;
        this.reviewedBy = reviewerId;
        this.reviewedAt = Instant.now();
    }
}
