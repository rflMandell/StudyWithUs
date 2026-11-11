package com.studywithus.challenge.presentation;

import com.studywithus.challenge.application.ChallengeParticipantResult;

import java.time.Instant;
import java.util.UUID;

public record ChallengeParticipantResponse(
        UUID challengeId,
        UUID userId,
        int progress,
        int targetValue,
        double percentage,
        boolean completed,
        Instant completedAt,
        Instant joinedAt
) {
    public static ChallengeParticipantResponse from(ChallengeParticipantResult result) {
        double pct = result.targetValue() > 0
                ? Math.min(100.0, Math.round(((double) result.progress() / result.targetValue() * 100.0) * 10.0) / 10.0)
                : 0.0;

        return new ChallengeParticipantResponse(
                result.challengeId(),
                result.userId(),
                result.progress(),
                result.targetValue(),
                pct,
                result.completed(),
                result.completedAt(),
                result.joinedAt()
        );
    }
}
