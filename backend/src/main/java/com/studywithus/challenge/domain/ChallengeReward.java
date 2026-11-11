package com.studywithus.challenge.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeReward {
    private UUID id;
    private UUID challengeId;
    private RewardType type;
    private Integer value;
    private UUID badgeId;

    public static ChallengeReward points(UUID challengeId, int points) {
        return ChallengeReward.builder()
                .id(UUID.randomUUID())
                .challengeId(challengeId)
                .type(RewardType.POINTS)
                .value(points)
                .build();
    }

    public static ChallengeReward badge(UUID challengeId, UUID badgeId) {
        return ChallengeReward.builder()
                .id(UUID.randomUUID())
                .challengeId(challengeId)
                .type(RewardType.BADGE)
                .badgeId(badgeId)
                .build();
    }
}
