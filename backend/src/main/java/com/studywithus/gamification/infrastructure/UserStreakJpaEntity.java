package com.studywithus.gamification.infrastructure;

import com.studywithus.gamification.domain.UserStreak;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "user_streaks")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStreakJpaEntity {

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "current_streak", nullable = false)
    private int currentStreak;

    @Column(name = "longest_streak", nullable = false)
    private int longestStreak;

    @Column(name = "last_activity_date")
    private LocalDate lastActivityDate;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UserStreak toDomain() {
        return UserStreak.builder()
                .userId(userId)
                .currentStreak(currentStreak)
                .longestStreak(longestStreak)
                .lastActivityDate(lastActivityDate)
                .updatedAt(updatedAt)
                .build();
    }

    public static UserStreakJpaEntity fromDomain(UserStreak domain) {
        return UserStreakJpaEntity.builder()
                .userId(domain.getUserId())
                .currentStreak(domain.getCurrentStreak())
                .longestStreak(domain.getLongestStreak())
                .lastActivityDate(domain.getLastActivityDate())
                .updatedAt(domain.getUpdatedAt() != null ? domain.getUpdatedAt() : Instant.now())
                .build();
    }
}
