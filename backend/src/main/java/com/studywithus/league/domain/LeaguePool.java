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
public class LeaguePool {
    public static final int MAX_PARTICIPANTS = 30;

    private UUID id;
    private UUID seasonId;
    private LeagueDivision division;
    private int poolNumber;
    private int participantsCount;
    private Instant createdAt;

    public static LeaguePool create(UUID seasonId, LeagueDivision division, int poolNumber) {
        return LeaguePool.builder()
                .id(UUID.randomUUID())
                .seasonId(seasonId)
                .division(division)
                .poolNumber(poolNumber)
                .participantsCount(0)
                .createdAt(Instant.now())
                .build();
    }

    public boolean hasCapacity() {
        return participantsCount < MAX_PARTICIPANTS;
    }

    public void incrementParticipants() {
        this.participantsCount++;
    }
}
