package com.studywithus.league.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LeagueLeaderboardCachePort {

    record LeaderboardScoreEntry(UUID userId, long score) {}

    void upsertScore(UUID poolId, UUID userId, long weeklyScore);

    Optional<List<LeaderboardScoreEntry>> getPoolLeaderboard(UUID poolId);

    void warmPoolLeaderboard(UUID poolId, List<LeaderboardScoreEntry> entries);

    void invalidatePool(UUID poolId);
}
