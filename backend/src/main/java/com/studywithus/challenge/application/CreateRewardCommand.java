package com.studywithus.challenge.application;

import com.studywithus.challenge.domain.RewardType;

import java.util.UUID;

public record CreateRewardCommand(
        RewardType type,
        Integer value,
        UUID badgeId
) {
}
