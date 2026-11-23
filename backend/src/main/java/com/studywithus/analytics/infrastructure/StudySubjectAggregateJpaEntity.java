package com.studywithus.analytics.infrastructure;

import com.studywithus.analytics.domain.StudySubjectAggregate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "study_subject_aggregates")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudySubjectAggregateJpaEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "subject", nullable = false, length = 100)
    private String subject;

    @Column(name = "total_minutes", nullable = false)
    private long totalMinutes;

    @Column(name = "sessions_count", nullable = false)
    private int sessionsCount;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static StudySubjectAggregateJpaEntity fromDomain(StudySubjectAggregate d) {
        return StudySubjectAggregateJpaEntity.builder()
                .id(d.getId())
                .userId(d.getUserId())
                .subject(d.getSubject())
                .totalMinutes(d.getTotalMinutes())
                .sessionsCount(d.getSessionsCount())
                .updatedAt(d.getUpdatedAt())
                .build();
    }

    public StudySubjectAggregate toDomain() {
        return StudySubjectAggregate.builder()
                .id(id)
                .userId(userId)
                .subject(subject)
                .totalMinutes(totalMinutes)
                .sessionsCount(sessionsCount)
                .updatedAt(updatedAt)
                .build();
    }
}
