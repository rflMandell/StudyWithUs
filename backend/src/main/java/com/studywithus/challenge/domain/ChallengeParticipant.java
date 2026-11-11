package com.studywithus.challenge.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeParticipant {
    private UUID challengeId;
    private UUID userId;
    private int progress;
    private Instant completedAt;
    private Instant joinedAt;

    public static ChallengeParticipant join(UUID challengeId, UUID userId) {
        return ChallengeParticipant.builder()
                .challengeId(challengeId)
                .userId(userId)
                .progress(0)
                .completedAt(null)
                .joinedAt(Instant.now())
                .build();
    }

    public boolean isCompleted() {
        return completedAt != null;
    }

    public boolean incrementProgress(int targetValue) {
        if (isCompleted()) {
            return false;
        }
        this.progress += 1;
        if (this.progress >= targetValue) {
            this.completedAt = Instant.now();
            return true;
        }
        return false;
    }
}
