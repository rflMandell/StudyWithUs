package com.studywithus.analytics.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudyDailyAggregate {
    private UUID id;
    private UUID userId;
    private LocalDate activityDate;
    private long totalMinutes;
    private int sessionsCount;
    private Instant updatedAt;

    public static StudyDailyAggregate create(UUID userId, LocalDate activityDate) {
        return StudyDailyAggregate.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .activityDate(activityDate)
                .totalMinutes(0L)
                .sessionsCount(0)
                .updatedAt(Instant.now())
                .build();
    }

    public void addSession(int durationMinutes) {
        this.totalMinutes += Math.max(durationMinutes, 0);
        this.sessionsCount++;
        this.updatedAt = Instant.now();
    }

    public void removeSession(int durationMinutes) {
        this.totalMinutes = Math.max(0L, this.totalMinutes - Math.max(durationMinutes, 0));
        this.sessionsCount = Math.max(0, this.sessionsCount - 1);
        this.updatedAt = Instant.now();
    }
}
