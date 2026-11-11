package com.studywithus.challenge.application;

import com.studywithus.challenge.domain.Challenge;
import com.studywithus.challenge.domain.ChallengeMetric;
import com.studywithus.challenge.domain.ChallengeParticipant;
import com.studywithus.challenge.domain.ChallengeProgressUpdatedEvent;
import com.studywithus.challenge.domain.ChallengeRepository;
import com.studywithus.challenge.domain.ChallengeReward;
import com.studywithus.challenge.domain.ChallengeStatus;
import com.studywithus.challenge.domain.RewardType;
import com.studywithus.checkin.domain.CheckinCreatedEvent;
import com.studywithus.gamification.application.AwardPointsCommand;
import com.studywithus.gamification.application.AwardPointsUseCase;
import com.studywithus.gamification.domain.ChallengeCompletedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChallengeProgressTrackerTest {

    @Mock
    private ChallengeRepository challengeRepository;

    @Mock
    private AwardPointsUseCase awardPointsUseCase;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ChallengeProgressTracker tracker;

    private UUID userId;
    private UUID communityId;
    private UUID challengeId;
    private UUID checkinId;
    private UUID badgeId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        communityId = UUID.randomUUID();
        challengeId = UUID.randomUUID();
        checkinId = UUID.randomUUID();
        badgeId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Increments progress on checkin but does not complete if below target")
    void shouldIncrementProgressWithoutCompleting() {
        ChallengeParticipant participant = ChallengeParticipant.builder()
                .challengeId(challengeId)
                .userId(userId)
                .progress(0)
                .build();

        Challenge challenge = Challenge.builder()
                .id(challengeId)
                .communityId(communityId)
                .targetValue(2)
                .metric(ChallengeMetric.CHECKINS)
                .status(ChallengeStatus.ACTIVE)
                .rewards(List.of(ChallengeReward.points(challengeId, 50)))
                .build();

        when(challengeRepository.findActiveParticipationsByUserAndCommunity(any(), any(), any()))
                .thenReturn(List.of(participant));
        when(challengeRepository.findById(challengeId)).thenReturn(Optional.of(challenge));

        tracker.onCheckinCreated(new CheckinCreatedEvent(checkinId, userId, communityId));

        assertThat(participant.getProgress()).isEqualTo(1);
        assertThat(participant.isCompleted()).isFalse();

        verify(challengeRepository).saveParticipant(participant);
        verify(eventPublisher).publishEvent(any(ChallengeProgressUpdatedEvent.class));
        verify(awardPointsUseCase, never()).execute(any());
        verify(eventPublisher, never()).publishEvent(any(ChallengeCompletedEvent.class));
    }

    @Test
    @DisplayName("Completes challenge and distributes points and badge rewards when reaching target")
    void shouldCompleteChallengeAndDistributeRewards() {
        ChallengeParticipant participant = ChallengeParticipant.builder()
                .challengeId(challengeId)
                .userId(userId)
                .progress(1)
                .build();

        Challenge challenge = Challenge.builder()
                .id(challengeId)
                .communityId(communityId)
                .targetValue(2)
                .metric(ChallengeMetric.CHECKINS)
                .status(ChallengeStatus.ACTIVE)
                .rewards(List.of(
                        ChallengeReward.points(challengeId, 50),
                        ChallengeReward.badge(challengeId, badgeId)
                ))
                .build();

        when(challengeRepository.findActiveParticipationsByUserAndCommunity(any(), any(), any()))
                .thenReturn(List.of(participant));
        when(challengeRepository.findById(challengeId)).thenReturn(Optional.of(challenge));

        tracker.onCheckinCreated(new CheckinCreatedEvent(checkinId, userId, communityId));

        assertThat(participant.getProgress()).isEqualTo(2);
        assertThat(participant.isCompleted()).isTrue();

        verify(challengeRepository).saveParticipant(participant);
        verify(awardPointsUseCase).execute(any(AwardPointsCommand.class));

        ArgumentCaptor<ChallengeCompletedEvent> eventCaptor = ArgumentCaptor.forClass(ChallengeCompletedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().challengeId()).isEqualTo(challengeId);
        assertThat(eventCaptor.getValue().userId()).isEqualTo(userId);
        assertThat(eventCaptor.getValue().badgeId()).isEqualTo(badgeId);
    }
}
