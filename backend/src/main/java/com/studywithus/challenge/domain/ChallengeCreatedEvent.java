package com.studywithus.challenge.domain;

import java.util.UUID;

public record ChallengeCreatedEvent(
        UUID challengeId,
        UUID creatorId,
        UUID communityId,
        String title
) {
}
