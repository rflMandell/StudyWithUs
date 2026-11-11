package com.studywithus.challenge.presentation;

import com.studywithus.challenge.application.RewardResult;
import com.studywithus.challenge.domain.RewardType;

import java.util.UUID;

public record ChallengeRewardResponse(
        UUID id,
        RewardType type,
        Integer value,
        UUID badgeId
) {
    public static ChallengeRewardResponse from(RewardResult result) {
        return new ChallengeRewardResponse(
                result.id(),
                result.type(),
                result.value(),
                result.badgeId()
        );
    }
}
