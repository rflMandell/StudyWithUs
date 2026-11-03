package com.studywithus.gamification.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStreak {
    private UUID userId;
    private int currentStreak;
    private int longestStreak;
    private LocalDate lastActivityDate;
    private Instant updatedAt;

    public static UserStreak empty(UUID userId) {
        return UserStreak.builder()
                .userId(userId)
                .currentStreak(0)
                .longestStreak(0)
                .lastActivityDate(null)
                .updatedAt(Instant.now())
                .build();
    }

    public void recordActivity(LocalDate activityDate, List<Integer> freeDays) {
        if (this.lastActivityDate == null) {
            this.currentStreak = 1;
            this.longestStreak = Math.max(this.longestStreak, 1);
            this.lastActivityDate = activityDate;
            this.updatedAt = Instant.now();
            return;
        }

        if (activityDate.equals(this.lastActivityDate)) {
            return;
        }

        if (activityDate.equals(this.lastActivityDate.plusDays(1))) {
            this.currentStreak++;
            this.longestStreak = Math.max(this.longestStreak, this.currentStreak);
            this.lastActivityDate = activityDate;
            this.updatedAt = Instant.now();
            return;
        }

        if (activityDate.isAfter(this.lastActivityDate.plusDays(1))) {
            boolean allGapDaysFree = true;
            if (freeDays != null && !freeDays.isEmpty()) {
                for (LocalDate d = this.lastActivityDate.plusDays(1); d.isBefore(activityDate); d = d.plusDays(1)) {
                    if (!freeDays.contains(d.getDayOfWeek().getValue())) {
                        allGapDaysFree = false;
                        break;
                    }
                }
            } else {
                allGapDaysFree = false;
            }

            if (allGapDaysFree) {
                this.currentStreak++;
                this.longestStreak = Math.max(this.longestStreak, this.currentStreak);
            } else {
                this.currentStreak = 1;
            }
            this.lastActivityDate = activityDate;
            this.updatedAt = Instant.now();
        }
    }
}
