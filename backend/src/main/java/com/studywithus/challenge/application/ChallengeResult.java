package com.studywithus.challenge.application;

import com.studywithus.challenge.domain.Challenge;
import com.studywithus.challenge.domain.ChallengeMetric;
import com.studywithus.challenge.domain.ChallengeScope;
import com.studywithus.challenge.domain.ChallengeStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ChallengeResult(
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
        List<RewardResult> rewards,
        Instant createdAt
) {
    public static ChallengeResult fromDomain(Challenge challenge) {
        List<RewardResult> rewardResults = challenge.getRewards() != null
                ? challenge.getRewards().stream().map(RewardResult::fromDomain).toList()
                : List.of();

        return new ChallengeResult(
                challenge.getId(),
                challenge.getCreatorId(),
                challenge.getCommunityId(),
                challenge.getTitle(),
                challenge.getDescription(),
                challenge.getScope(),
                challenge.getMetric(),
                challenge.getTargetValue(),
                challenge.getStartAt(),
                challenge.getEndAt(),
                challenge.getStatus(),
                rewardResults,
                challenge.getCreatedAt()
        );
    }
}
