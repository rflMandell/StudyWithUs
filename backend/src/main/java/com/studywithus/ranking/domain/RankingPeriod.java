package com.studywithus.ranking.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RankingPeriod {
    private UUID id;
    private UUID communityId;
    private UUID configurationId;
    private Instant startsAt;
    private Instant endsAt;
    private RankingPeriodStatus status;
    private Instant createdAt;

    public static RankingPeriod createCurrentWeekly(UUID communityId, UUID configurationId) {
        Instant now = Instant.now();
        Instant startsAt = now.truncatedTo(ChronoUnit.DAYS);
        Instant endsAt = startsAt.plus(7, ChronoUnit.DAYS);

        return RankingPeriod.builder()
                .id(UUID.randomUUID())
                .communityId(communityId)
                .configurationId(configurationId)
                .startsAt(startsAt)
                .endsAt(endsAt)
                .status(RankingPeriodStatus.ACTIVE)
                .createdAt(now)
                .build();
    }

    public boolean isActive() {
        return status == RankingPeriodStatus.ACTIVE;
    }
}
