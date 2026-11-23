package com.studywithus.league.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeagueSeasonHistory {
    private UUID id;
    private UUID seasonId;
    private String weekKey;
    private UUID userId;
    private UUID poolId;
    private LeagueDivision division;
    private LeagueDivision nextDivision;
    private int finalRank;
    private long weeklyScore;
    private LeagueOutcome outcome;
    private Instant recordedAt;

    public static LeagueSeasonHistory create(
            LeagueSeason season,
            LeagueParticipant participant,
            LeagueDivision nextDivision,
            int finalRank,
            LeagueOutcome outcome
    ) {
        return LeagueSeasonHistory.builder()
                .id(UUID.randomUUID())
                .seasonId(season.getId())
                .weekKey(season.getWeekKey())
                .userId(participant.getUserId())
                .poolId(participant.getPoolId())
                .division(participant.getDivision())
                .nextDivision(nextDivision)
                .finalRank(finalRank)
                .weeklyScore(participant.getWeeklyScore())
                .outcome(outcome)
                .recordedAt(Instant.now())
                .build();
    }
}
