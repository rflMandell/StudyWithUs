package com.studywithus.challenge.application;

import java.util.UUID;

public record GetChallengeProgressQuery(
        UUID challengeId,
        UUID userId
) {
}
