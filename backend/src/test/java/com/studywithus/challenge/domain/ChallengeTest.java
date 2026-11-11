package com.studywithus.challenge.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ChallengeTest {

    @Test
    @DisplayName("Should increment participant progress and return true on completion")
    void shouldIncrementParticipantProgress() {
        ChallengeParticipant participant = ChallengeParticipant.join(UUID.randomUUID(), UUID.randomUUID());
        assertThat(participant.getProgress()).isZero();
        assertThat(participant.isCompleted()).isFalse();

        boolean completedOnFirst = participant.incrementProgress(2);
        assertThat(completedOnFirst).isFalse();
        assertThat(participant.getProgress()).isEqualTo(1);
        assertThat(participant.isCompleted()).isFalse();
        assertThat(participant.getCompletedAt()).isNull();

        boolean completedOnSecond = participant.incrementProgress(2);
        assertThat(completedOnSecond).isTrue();
        assertThat(participant.getProgress()).isEqualTo(2);
        assertThat(participant.isCompleted()).isTrue();
        assertThat(participant.getCompletedAt()).isNotNull();

        // Incrementing after completion should do nothing
        boolean completedAfter = participant.incrementProgress(2);
        assertThat(completedAfter).isFalse();
        assertThat(participant.getProgress()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should correctly evaluate challenge active status")
    void shouldEvaluateActiveStatus() {
        Instant now = Instant.now();
        Challenge challenge = Challenge.builder()
                .id(UUID.randomUUID())
                .status(ChallengeStatus.ACTIVE)
                .startAt(now.minus(1, ChronoUnit.DAYS))
                .endAt(now.plus(1, ChronoUnit.DAYS))
                .build();

        assertThat(challenge.isActive(now)).isTrue();

        // Expired
        challenge.setEndAt(now.minus(1, ChronoUnit.HOURS));
        assertThat(challenge.isActive(now)).isFalse();

        // Cancelled
        challenge.setEndAt(now.plus(1, ChronoUnit.DAYS));
        challenge.cancel();
        assertThat(challenge.isActive(now)).isFalse();
        assertThat(challenge.getStatus()).isEqualTo(ChallengeStatus.CANCELLED);
    }
}
