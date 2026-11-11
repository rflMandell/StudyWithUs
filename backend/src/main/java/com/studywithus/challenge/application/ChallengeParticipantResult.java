package com.studywithus.challenge.application;

import com.studywithus.challenge.domain.ChallengeParticipant;

import java.time.Instant;
import java.util.UUID;

public record ChallengeParticipantResult(
        UUID challengeId,
        UUID userId,
        int progress,
        int targetValue,
        boolean completed,
        Instant completedAt,
        Instant joinedAt
) {
    public static ChallengeParticipantResult fromDomain(ChallengeParticipant participant, int targetValue) {
        return new ChallengeParticipantResult(
                participant.getChallengeId(),
                participant.getUserId(),
                participant.getProgress(),
                targetValue,
                participant.isCompleted(),
                participant.getCompletedAt(),
                participant.getJoinedAt()
        );
    }
}
