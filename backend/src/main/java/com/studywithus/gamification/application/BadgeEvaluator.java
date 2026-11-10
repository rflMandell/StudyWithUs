package com.studywithus.gamification.application;

import com.studywithus.checkin.domain.CheckinCreatedEvent;
import com.studywithus.community.domain.CommunityCreatedEvent;
import com.studywithus.gamification.domain.BadgeConstants;
import com.studywithus.gamification.domain.ChallengeCompletedEvent;
import com.studywithus.gamification.domain.PointTransactionRepository;
import com.studywithus.gamification.domain.PointsAwardedEvent;
import com.studywithus.gamification.domain.StreakUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BadgeEvaluator {

    private final AwardBadgeUseCase awardBadgeUseCase;
    private final PointTransactionRepository pointTransactionRepository;

    @EventListener
    public void onStreakUpdated(StreakUpdatedEvent event) {
        log.debug("Evaluating streak badge criteria for user: {}, userStreak: {}, commStreak: {}",
                event.userId(), event.userCurrentStreak(), event.communityCurrentStreak());

        int maxStreak = Math.max(event.userCurrentStreak(), event.communityCurrentStreak());

        if (maxStreak >= 7) {
            awardBadgeUseCase.execute(new AwardBadgeCommand(
                    event.userId(),
                    BadgeConstants.BADGE_STREAK_7_ID,
                    event.communityId()
            ));
        }

        if (maxStreak >= 30) {
            awardBadgeUseCase.execute(new AwardBadgeCommand(
                    event.userId(),
                    BadgeConstants.BADGE_STREAK_30_ID,
                    event.communityId()
            ));
        }
    }

    @EventListener
    public void onCheckinCreated(CheckinCreatedEvent event) {
        log.debug("Evaluating first checkin badge criteria for user: {}", event.userId());
        awardBadgeUseCase.execute(new AwardBadgeCommand(
                event.userId(),
                BadgeConstants.BADGE_FIRST_CHECKIN_ID,
                event.communityId()
        ));
    }

    @EventListener
    public void onPointsAwarded(PointsAwardedEvent event) {
        log.debug("Evaluating points badge criteria for user: {}", event.userId());
        long totalPoints = pointTransactionRepository.calculateTotalPointsByUser(event.userId());
        if (totalPoints >= 100) {
            awardBadgeUseCase.execute(new AwardBadgeCommand(
                    event.userId(),
                    BadgeConstants.BADGE_POINTS_100_ID,
                    event.communityId()
            ));
        }
    }

    @EventListener
    public void onCommunityCreated(CommunityCreatedEvent event) {
        log.debug("Evaluating community creator badge criteria for user: {}", event.ownerId());
        awardBadgeUseCase.execute(new AwardBadgeCommand(
                event.ownerId(),
                BadgeConstants.BADGE_COMMUNITY_CREATOR_ID,
                event.communityId()
        ));
    }

    @EventListener
    public void onChallengeCompleted(ChallengeCompletedEvent event) {
        if (event.badgeId() != null) {
            log.debug("Evaluating challenge badge criteria for user: {}, challenge: {}", event.userId(), event.challengeId());
            awardBadgeUseCase.execute(new AwardBadgeCommand(
                    event.userId(),
                    event.badgeId(),
                    event.challengeId(),
                    null
            ));
        }
    }
}
