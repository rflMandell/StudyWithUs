package com.studywithus.challenge.presentation;

import com.studywithus.challenge.application.CreateChallengeCommand;
import com.studywithus.challenge.application.CreateRewardCommand;
import com.studywithus.challenge.domain.ChallengeMetric;
import com.studywithus.challenge.domain.ChallengeScope;
import com.studywithus.challenge.domain.RewardType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record CreateChallengeRequest(
        @NotBlank(message = "O título é obrigatório")
        @Size(max = 200, message = "O título deve ter no máximo 200 caracteres")
        String title,

        @Size(max = 1000, message = "A descrição deve ter no máximo 1000 caracteres")
        String description,

        ChallengeScope scope,

        ChallengeMetric metric,

        @Min(value = 1, message = "A meta deve ser no mínimo 1")
        int targetValue,

        Instant startAt,

        Instant endAt,

        Integer pointsReward,

        UUID badgeReward
) {
    public CreateChallengeCommand toCommand(UUID creatorId, UUID communityId) {
        List<CreateRewardCommand> rewardCommands = new ArrayList<>();
        if (pointsReward != null && pointsReward > 0) {
            rewardCommands.add(new CreateRewardCommand(RewardType.POINTS, pointsReward, null));
        }
        if (badgeReward != null) {
            rewardCommands.add(new CreateRewardCommand(RewardType.BADGE, null, badgeReward));
        }

        return new CreateChallengeCommand(
                creatorId,
                communityId,
                title,
                description,
                scope != null ? scope : ChallengeScope.INDIVIDUAL,
                metric != null ? metric : ChallengeMetric.CHECKINS,
                targetValue,
                startAt,
                endAt,
                rewardCommands
        );
    }
}
