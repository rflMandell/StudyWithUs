package com.studywithus.league.infrastructure;

import com.studywithus.league.domain.LeagueDivision;
import com.studywithus.league.domain.LeagueOutcome;
import com.studywithus.league.domain.LeagueParticipant;
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
@Table(name = "league_participants")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeagueParticipantJpaEntity {

    @Id
    private UUID id;

    @Column(name = "pool_id", nullable = false)
    private UUID poolId;

    @Column(name = "season_id", nullable = false)
    private UUID seasonId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "division", nullable = false, length = 20)
    private LeagueDivision division;

    @Column(name = "weekly_score", nullable = false)
    private long weeklyScore;

    @Column(name = "final_rank")
    private Integer finalRank;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", length = 20)
    private LeagueOutcome outcome;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static LeagueParticipantJpaEntity fromDomain(LeagueParticipant d) {
        return LeagueParticipantJpaEntity.builder()
                .id(d.getId())
                .poolId(d.getPoolId())
                .seasonId(d.getSeasonId())
                .userId(d.getUserId())
                .division(d.getDivision())
                .weeklyScore(d.getWeeklyScore())
                .finalRank(d.getFinalRank())
                .outcome(d.getOutcome())
                .joinedAt(d.getJoinedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }

    public LeagueParticipant toDomain() {
        return LeagueParticipant.builder()
                .id(id)
                .poolId(poolId)
                .seasonId(seasonId)
                .userId(userId)
                .division(division)
                .weeklyScore(weeklyScore)
                .finalRank(finalRank)
                .outcome(outcome)
                .joinedAt(joinedAt)
                .updatedAt(updatedAt)
                .build();
    }
}
