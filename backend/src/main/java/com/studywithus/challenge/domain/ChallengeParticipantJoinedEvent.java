package com.studywithus.challenge.domain;

import java.util.UUID;

public record ChallengeParticipantJoinedEvent(
        UUID challengeId,
        UUID userId
) {
}
