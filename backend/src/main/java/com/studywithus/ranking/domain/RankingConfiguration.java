package com.studywithus.ranking.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RankingConfiguration {
    private UUID id;
    private UUID communityId;
    private RankingPeriodType periodType;
    private String timezone;
    private LocalDate startDate;
    private LocalDate endDate;
    private Instant createdAt;
    private Instant updatedAt;

    public static RankingConfiguration createDefault(UUID communityId, String timezone) {
        Instant now = Instant.now();
        return RankingConfiguration.builder()
                .id(UUID.randomUUID())
                .communityId(communityId)
                .periodType(RankingPeriodType.WEEKLY)
                .timezone(timezone != null ? timezone : "UTC")
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}
