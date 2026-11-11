package com.studywithus.challenge.application;

import java.util.UUID;

public record JoinChallengeCommand(
        UUID challengeId,
        UUID userId
) {
}
