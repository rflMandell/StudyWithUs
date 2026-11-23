package com.studywithus.analytics.infrastructure;

import com.studywithus.analytics.domain.StudyDailyAggregate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "study_daily_aggregates")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudyDailyAggregateJpaEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "activity_date", nullable = false)
    private LocalDate activityDate;

    @Column(name = "total_minutes", nullable = false)
    private long totalMinutes;

    @Column(name = "sessions_count", nullable = false)
    private int sessionsCount;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static StudyDailyAggregateJpaEntity fromDomain(StudyDailyAggregate d) {
        return StudyDailyAggregateJpaEntity.builder()
                .id(d.getId())
                .userId(d.getUserId())
                .activityDate(d.getActivityDate())
                .totalMinutes(d.getTotalMinutes())
                .sessionsCount(d.getSessionsCount())
                .updatedAt(d.getUpdatedAt())
                .build();
    }

    public StudyDailyAggregate toDomain() {
        return StudyDailyAggregate.builder()
                .id(id)
                .userId(userId)
                .activityDate(activityDate)
                .totalMinutes(totalMinutes)
                .sessionsCount(sessionsCount)
                .updatedAt(updatedAt)
                .build();
    }
}
