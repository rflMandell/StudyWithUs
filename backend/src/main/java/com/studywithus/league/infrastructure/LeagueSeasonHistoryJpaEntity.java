package com.studywithus.league.infrastructure;

import com.studywithus.league.domain.LeagueDivision;
import com.studywithus.league.domain.LeagueOutcome;
import com.studywithus.league.domain.LeagueSeasonHistory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "league_season_history")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeagueSeasonHistoryJpaEntity {

    @Id
    private UUID id;

    @Column(name = "season_id", nullable = false)
    private UUID seasonId;

    @Column(name = "week_key", nullable = false, length = 20)
    private String weekKey;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "pool_id", nullable = false)
    private UUID poolId;

    @Enumerated(EnumType.STRING)
    @Column(name = "division", nullable = false, length = 20)
    private LeagueDivision division;

    @Enumerated(EnumType.STRING)
    @Column(name = "next_division", nullable = false, length = 20)
    private LeagueDivision nextDivision;

    @Column(name = "final_rank", nullable = false)
    private int finalRank;

    @Column(name = "weekly_score", nullable = false)
    private long weeklyScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false, length = 20)
    private LeagueOutcome outcome;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    public static LeagueSeasonHistoryJpaEntity fromDomain(LeagueSeasonHistory d) {
        return LeagueSeasonHistoryJpaEntity.builder()
                .id(d.getId())
                .seasonId(d.getSeasonId())
                .weekKey(d.getWeekKey())
                .userId(d.getUserId())
                .poolId(d.getPoolId())
                .division(d.getDivision())
                .nextDivision(d.getNextDivision())
                .finalRank(d.getFinalRank())
                .weeklyScore(d.getWeeklyScore())
                .outcome(d.getOutcome())
                .recordedAt(d.getRecordedAt())
                .build();
    }

    public LeagueSeasonHistory toDomain() {
        return LeagueSeasonHistory.builder()
                .id(id)
                .seasonId(seasonId)
                .weekKey(weekKey)
                .userId(userId)
                .poolId(poolId)
                .division(division)
                .nextDivision(nextDivision)
                .finalRank(finalRank)
                .weeklyScore(weeklyScore)
                .outcome(outcome)
                .recordedAt(recordedAt)
                .build();
    }
}
