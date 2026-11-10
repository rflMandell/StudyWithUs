package com.studywithus.gamification.application;

import com.studywithus.checkin.domain.CheckinCreatedEvent;
import com.studywithus.community.domain.CommunityCreatedEvent;
import com.studywithus.gamification.domain.BadgeConstants;
import com.studywithus.gamification.domain.PointTransactionRepository;
import com.studywithus.gamification.domain.PointTransactionType;
import com.studywithus.gamification.domain.PointsAwardedEvent;
import com.studywithus.gamification.domain.StreakUpdatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BadgeEvaluatorTest {

    @Mock
    private AwardBadgeUseCase awardBadgeUseCase;

    @Mock
    private PointTransactionRepository pointTransactionRepository;

    private BadgeEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new BadgeEvaluator(awardBadgeUseCase, pointTransactionRepository);
    }

    @Test
    @DisplayName("should trigger 7-day streak badge when streak reaches 7")
    void shouldTrigger7DayBadgeOnStreak7() {
        UUID userId = UUID.randomUUID();
        UUID communityId = UUID.randomUUID();

        evaluator.onStreakUpdated(new StreakUpdatedEvent(userId, communityId, 7, 7, LocalDate.now()));

        ArgumentCaptor<AwardBadgeCommand> captor = ArgumentCaptor.forClass(AwardBadgeCommand.class);
        verify(awardBadgeUseCase).execute(captor.capture());

        assertThat(captor.getValue().userId()).isEqualTo(userId);
        assertThat(captor.getValue().badgeId()).isEqualTo(BadgeConstants.BADGE_STREAK_7_ID);
    }

    @Test
    @DisplayName("should trigger first checkin badge on CheckinCreatedEvent")
    void shouldTriggerFirstCheckinBadge() {
        UUID userId = UUID.randomUUID();
        UUID checkinId = UUID.randomUUID();
        UUID communityId = UUID.randomUUID();

        evaluator.onCheckinCreated(new CheckinCreatedEvent(checkinId, userId, communityId));

        ArgumentCaptor<AwardBadgeCommand> captor = ArgumentCaptor.forClass(AwardBadgeCommand.class);
        verify(awardBadgeUseCase).execute(captor.capture());

        assertThat(captor.getValue().userId()).isEqualTo(userId);
        assertThat(captor.getValue().badgeId()).isEqualTo(BadgeConstants.BADGE_FIRST_CHECKIN_ID);
    }

    @Test
    @DisplayName("should trigger community creator badge on CommunityCreatedEvent")
    void shouldTriggerCommunityCreatorBadge() {
        UUID ownerId = UUID.randomUUID();
        UUID communityId = UUID.randomUUID();

        evaluator.onCommunityCreated(new CommunityCreatedEvent(communityId, "Grupo de Redes", ownerId, Instant.now()));

        ArgumentCaptor<AwardBadgeCommand> captor = ArgumentCaptor.forClass(AwardBadgeCommand.class);
        verify(awardBadgeUseCase).execute(captor.capture());

        assertThat(captor.getValue().userId()).isEqualTo(ownerId);
        assertThat(captor.getValue().badgeId()).isEqualTo(BadgeConstants.BADGE_COMMUNITY_CREATOR_ID);
    }

    @Test
    @DisplayName("should trigger 100 points badge when user total points reach 100")
    void shouldTrigger100PointsBadge() {
        UUID userId = UUID.randomUUID();
        UUID communityId = UUID.randomUUID();

        when(pointTransactionRepository.calculateTotalPointsByUser(userId)).thenReturn(100L);

        evaluator.onPointsAwarded(new PointsAwardedEvent(UUID.randomUUID(), userId, communityId, 10, PointTransactionType.CHECKIN));

        ArgumentCaptor<AwardBadgeCommand> captor = ArgumentCaptor.forClass(AwardBadgeCommand.class);
        verify(awardBadgeUseCase).execute(captor.capture());

        assertThat(captor.getValue().userId()).isEqualTo(userId);
        assertThat(captor.getValue().badgeId()).isEqualTo(BadgeConstants.BADGE_POINTS_100_ID);
    }
}
