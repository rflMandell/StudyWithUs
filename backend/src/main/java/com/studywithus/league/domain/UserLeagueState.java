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
public class UserLeagueState {
    private UUID userId;
    private LeagueDivision currentDivision;
    private LeagueDivision highestDivision;
    private Instant updatedAt;

    public static UserLeagueState initial(UUID userId) {
        return UserLeagueState.builder()
                .userId(userId)
                .currentDivision(LeagueDivision.BRONZE)
                .highestDivision(LeagueDivision.BRONZE)
                .updatedAt(Instant.now())
                .build();
    }

    public void transitionTo(LeagueDivision nextDivision) {
        this.currentDivision = nextDivision;
        if (nextDivision.isHigherThan(this.highestDivision)) {
            this.highestDivision = nextDivision;
        }
        this.updatedAt = Instant.now();
    }
}
