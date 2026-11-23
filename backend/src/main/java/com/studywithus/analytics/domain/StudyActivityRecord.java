package com.studywithus.analytics.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudyActivityRecord {
    private UUID checkinId;
    private UUID userId;
    private LocalDate activityDate;
    private String subject;
    private int durationMinutes;
    private Instant createdAt;

    public static StudyActivityRecord create(UUID checkinId, UUID userId, LocalDate activityDate, String subject, int durationMinutes) {
        return StudyActivityRecord.builder()
                .checkinId(checkinId)
                .userId(userId)
                .activityDate(activityDate)
                .subject(subject)
                .durationMinutes(Math.max(durationMinutes, 0))
                .createdAt(Instant.now())
                .build();
    }
}
