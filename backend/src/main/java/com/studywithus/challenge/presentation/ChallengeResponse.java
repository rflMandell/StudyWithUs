package com.studywithus.challenge.presentation;

import com.studywithus.challenge.application.ChallengeResult;
import com.studywithus.challenge.domain.ChallengeMetric;
import com.studywithus.challenge.domain.ChallengeScope;
import com.studywithus.challenge.domain.ChallengeStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ChallengeResponse(
        UUID id,
        UUID creatorId,
        UUID communityId,
        String title,
        String description,
        ChallengeScope scope,
        ChallengeMetric metric,
        int targetValue,
        Instant startAt,
        Instant endAt,
        ChallengeStatus status,
        List<ChallengeRewardResponse> rewards,
        Instant createdAt
) {
    public static ChallengeResponse from(ChallengeResult result) {
        List<ChallengeRewardResponse> rewardResponses = result.rewards() != null
                ? result.rewards().stream().map(ChallengeRewardResponse::from).toList()
                : List.of();

        return new ChallengeResponse(
                result.id(),
                result.creatorId(),
                result.communityId(),
                result.title(),
                result.description(),
                result.scope(),
                result.metric(),
                result.targetValue(),
                result.startAt(),
                result.endAt(),
                result.status(),
                rewardResponses,
                result.createdAt()
        );
    }
}
