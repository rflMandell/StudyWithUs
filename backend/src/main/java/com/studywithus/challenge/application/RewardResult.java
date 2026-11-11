package com.studywithus.challenge.application;

import com.studywithus.challenge.domain.ChallengeReward;
import com.studywithus.challenge.domain.RewardType;

import java.util.UUID;

public record RewardResult(
        UUID id,
        RewardType type,
        Integer value,
        UUID badgeId
) {
    public static RewardResult fromDomain(ChallengeReward reward) {
        return new RewardResult(
                reward.getId(),
                reward.getType(),
                reward.getValue(),
                reward.getBadgeId()
        );
    }
}
