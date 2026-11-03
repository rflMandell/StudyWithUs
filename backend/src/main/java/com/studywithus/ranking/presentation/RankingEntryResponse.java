package com.studywithus.ranking.presentation;

import com.studywithus.ranking.application.RankingEntryResult;

import java.time.Instant;
import java.util.UUID;

public record RankingEntryResponse(
        UUID rankingPeriodId,
        UUID userId,
        String username,
        String name,
        String avatarUrl,
        long points,
        int position,
        int streakAtUpdate,
        Instant updatedAt
) {
    public static RankingEntryResponse from(RankingEntryResult result) {
        return new RankingEntryResponse(
                result.rankingPeriodId(),
                result.userId(),
                result.username(),
                result.name(),
                result.avatarUrl(),
                result.points(),
                result.position(),
                result.streakAtUpdate(),
                result.updatedAt()
        );
    }
}
