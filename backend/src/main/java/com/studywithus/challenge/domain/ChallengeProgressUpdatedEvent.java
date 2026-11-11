package com.studywithus.challenge.domain;

import java.util.UUID;

public record ChallengeProgressUpdatedEvent(
        UUID challengeId,
        UUID userId,
        int oldProgress,
        int newProgress,
        int targetValue
) {
}
