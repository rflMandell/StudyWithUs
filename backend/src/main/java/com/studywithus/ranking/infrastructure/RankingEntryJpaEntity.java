package com.studywithus.ranking.infrastructure;

import com.studywithus.ranking.domain.RankingEntry;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ranking_entries")
@IdClass(RankingEntryId.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RankingEntryJpaEntity {

    @Id
    @Column(name = "ranking_period_id", nullable = false)
    private UUID rankingPeriodId;

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private long points;

    @Column(nullable = false)
    private int position;

    @Column(name = "streak_at_update", nullable = false)
    private int streakAtUpdate;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static RankingEntryJpaEntity fromDomain(RankingEntry domain) {
        return RankingEntryJpaEntity.builder()
                .rankingPeriodId(domain.getRankingPeriodId())
                .userId(domain.getUserId())
                .points(domain.getPoints())
                .position(domain.getPosition())
                .streakAtUpdate(domain.getStreakAtUpdate())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    public RankingEntry toDomain() {
        return RankingEntry.builder()
                .rankingPeriodId(this.rankingPeriodId)
                .userId(this.userId)
                .points(this.points)
                .position(this.position)
                .streakAtUpdate(this.streakAtUpdate)
                .updatedAt(this.updatedAt)
                .build();
    }
}
