package com.studywithus.ranking.application;

import java.util.UUID;

public record UserRankingPositionResult(
        UUID communityId,
        UUID rankingPeriodId,
        UUID userId,
        long points,
        int position,
        int streakAtUpdate
) {
}
