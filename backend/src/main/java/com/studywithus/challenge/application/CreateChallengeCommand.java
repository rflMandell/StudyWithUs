package com.studywithus.challenge.application;

import com.studywithus.challenge.domain.ChallengeMetric;
import com.studywithus.challenge.domain.ChallengeScope;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CreateChallengeCommand(
        UUID creatorId,
        UUID communityId,
        String title,
        String description,
        ChallengeScope scope,
        ChallengeMetric metric,
        int targetValue,
        Instant startAt,
        Instant endAt,
        List<CreateRewardCommand> rewards
) {
    public CreateChallengeCommand {
        if (scope == null) {
            scope = ChallengeScope.INDIVIDUAL;
        }
        if (metric == null) {
            metric = ChallengeMetric.CHECKINS;
        }
        if (rewards == null) {
            rewards = List.of();
        }
    }
}
