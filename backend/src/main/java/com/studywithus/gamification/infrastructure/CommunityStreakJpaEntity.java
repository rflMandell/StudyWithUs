package com.studywithus.gamification.infrastructure;

import com.studywithus.gamification.domain.CommunityStreak;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "community_streaks")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityStreakJpaEntity {

    @EmbeddedId
    private CommunityStreakId id;

    @Column(name = "current_streak", nullable = false)
    private int currentStreak;

    @Column(name = "longest_streak", nullable = false)
    private int longestStreak;

    @Column(name = "last_activity_date")
    private LocalDate lastActivityDate;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public CommunityStreak toDomain() {
        return CommunityStreak.builder()
                .communityId(id.getCommunityId())
                .userId(id.getUserId())
                .currentStreak(currentStreak)
                .longestStreak(longestStreak)
                .lastActivityDate(lastActivityDate)
                .updatedAt(updatedAt)
                .build();
    }

    public static CommunityStreakJpaEntity fromDomain(CommunityStreak domain) {
        return CommunityStreakJpaEntity.builder()
                .id(new CommunityStreakId(domain.getCommunityId(), domain.getUserId()))
                .currentStreak(domain.getCurrentStreak())
                .longestStreak(domain.getLongestStreak())
                .lastActivityDate(domain.getLastActivityDate())
                .updatedAt(domain.getUpdatedAt() != null ? domain.getUpdatedAt() : Instant.now())
                .build();
    }
}
