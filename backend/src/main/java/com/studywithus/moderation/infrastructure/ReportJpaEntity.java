package com.studywithus.moderation.infrastructure;

import com.studywithus.moderation.domain.Report;
import com.studywithus.moderation.domain.ReportStatus;
import com.studywithus.moderation.domain.ReportTargetType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reports")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportJpaEntity {

    @Id
    private UUID id;

    @Column(name = "reporter_id", nullable = false)
    private UUID reporterId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 30)
    private ReportTargetType targetType;

    @Column(name = "target_id", nullable = false)
    private UUID targetId;

    @Column(nullable = false, length = 100)
    private String reason;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportStatus status;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public static ReportJpaEntity fromDomain(Report domain) {
        return ReportJpaEntity.builder()
                .id(domain.getId())
                .reporterId(domain.getReporterId())
                .targetType(domain.getTargetType())
                .targetId(domain.getTargetId())
                .reason(domain.getReason())
                .description(domain.getDescription())
                .status(domain.getStatus())
                .reviewedBy(domain.getReviewedBy())
                .reviewedAt(domain.getReviewedAt())
                .createdAt(domain.getCreatedAt() != null ? domain.getCreatedAt() : Instant.now())
                .build();
    }

    public Report toDomain() {
        return Report.builder()
                .id(this.id)
                .reporterId(this.reporterId)
                .targetType(this.targetType)
                .targetId(this.targetId)
                .reason(this.reason)
                .description(this.description)
                .status(this.status)
                .reviewedBy(this.reviewedBy)
                .reviewedAt(this.reviewedAt)
                .createdAt(this.createdAt)
                .build();
    }
}
