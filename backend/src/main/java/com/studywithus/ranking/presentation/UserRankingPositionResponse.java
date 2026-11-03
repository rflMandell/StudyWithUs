package com.studywithus.ranking.presentation;

import com.studywithus.ranking.application.UserRankingPositionResult;

import java.util.UUID;

public record UserRankingPositionResponse(
        UUID communityId,
        UUID rankingPeriodId,
        UUID userId,
        long points,
        int position,
        int streakAtUpdate
) {
    public static UserRankingPositionResponse from(UserRankingPositionResult result) {
        return new UserRankingPositionResponse(
                result.communityId(),
                result.rankingPeriodId(),
                result.userId(),
                result.points(),
                result.position(),
                result.streakAtUpdate()
        );
    }
}
