package com.studywithus.ranking.application;

import com.studywithus.ranking.domain.RankingEntry;

import java.time.Instant;
import java.util.UUID;

public record RankingEntryResult(
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
    public static RankingEntryResult of(
            RankingEntry entry,
            String username,
            String name,
            String avatarUrl
    ) {
        return new RankingEntryResult(
                entry.getRankingPeriodId(),
                entry.getUserId(),
                username,
                name,
                avatarUrl,
                entry.getPoints(),
                entry.getPosition(),
                entry.getStreakAtUpdate(),
                entry.getUpdatedAt()
        );
    }
}
