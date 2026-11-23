package com.studywithus.league.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeagueParticipant {
    private UUID id;
    private UUID poolId;
    private UUID seasonId;
    private UUID userId;
    private LeagueDivision division;
    private long weeklyScore;
    private Integer finalRank;
    private LeagueOutcome outcome;
    private Instant joinedAt;
    private Instant updatedAt;

    public static LeagueParticipant create(UUID poolId, UUID seasonId, UUID userId, LeagueDivision division) {
        Instant now = Instant.now();
        return LeagueParticipant.builder()
                .id(UUID.randomUUID())
                .poolId(poolId)
                .seasonId(seasonId)
                .userId(userId)
                .division(division)
                .weeklyScore(0L)
                .joinedAt(now)
                .updatedAt(now)
                .build();
    }

    public void addScore(long delta) {
        this.weeklyScore = Math.max(0L, this.weeklyScore + delta);
        this.updatedAt = Instant.now();
    }

    public void finalizeSeason(int rank, LeagueOutcome outcome) {
        this.finalRank = rank;
        this.outcome = outcome;
        this.updatedAt = Instant.now();
    }
}
