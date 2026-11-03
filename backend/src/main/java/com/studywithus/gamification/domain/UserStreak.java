package com.studywithus.gamification.domain;

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
}
