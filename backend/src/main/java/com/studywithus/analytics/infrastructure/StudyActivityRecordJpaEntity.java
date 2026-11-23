package com.studywithus.analytics.infrastructure;

import com.studywithus.analytics.domain.StudyActivityRecord;
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
@Table(name = "study_activity_records")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudyActivityRecordJpaEntity {

    @Id
    @Column(name = "checkin_id")
    private UUID checkinId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "activity_date", nullable = false)
    private LocalDate activityDate;

    @Column(name = "subject", nullable = false, length = 100)
    private String subject;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public static StudyActivityRecordJpaEntity fromDomain(StudyActivityRecord d) {
        return StudyActivityRecordJpaEntity.builder()
                .checkinId(d.getCheckinId())
                .userId(d.getUserId())
                .activityDate(d.getActivityDate())
                .subject(d.getSubject())
                .durationMinutes(d.getDurationMinutes())
                .createdAt(d.getCreatedAt())
                .build();
    }

    public StudyActivityRecord toDomain() {
        return StudyActivityRecord.builder()
                .checkinId(checkinId)
                .userId(userId)
                .activityDate(activityDate)
                .subject(subject)
                .durationMinutes(durationMinutes)
                .createdAt(createdAt)
                .build();
    }
}
