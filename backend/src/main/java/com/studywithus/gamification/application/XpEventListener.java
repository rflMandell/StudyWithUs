package com.studywithus.gamification.application;

import com.studywithus.checkin.domain.CheckinCreatedEvent;
import com.studywithus.gamification.domain.ChallengeCompletedEvent;
import com.studywithus.gamification.domain.StreakUpdatedEvent;
import com.studywithus.gamification.domain.XpSourceType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class XpEventListener {

    public static final long CHECKIN_XP = 50L;
    public static final long STREAK_BONUS_XP = 25L;
    public static final long CHALLENGE_COMPLETED_XP = 150L;

    private final AwardXpUseCase awardXpUseCase;

    @EventListener
    public void onCheckinCreated(CheckinCreatedEvent event) {
        try {
            awardXpUseCase.execute(new AwardXpCommand(
                    event.userId(),
                    XpSourceType.CHECKIN,
                    event.checkinId().toString(),
                    CHECKIN_XP
            ));
        } catch (Exception e) {
            log.error("Failed to award XP for checkinId={}: {}", event.checkinId(), e.getMessage());
        }
    }

    @EventListener
    public void onStreakUpdated(StreakUpdatedEvent event) {
        if (event.userCurrentStreak() < 2) {
            return;
        }
        try {
            String sourceId = "streak:" + event.activityDate();
            awardXpUseCase.execute(new AwardXpCommand(
                    event.userId(),
                    XpSourceType.STREAK_MILESTONE,
                    sourceId,
                    STREAK_BONUS_XP
            ));
        } catch (Exception e) {
            log.error("Failed to award streak XP for userId={}: {}", event.userId(), e.getMessage());
        }
    }

    @EventListener
    public void onChallengeCompleted(ChallengeCompletedEvent event) {
        try {
            awardXpUseCase.execute(new AwardXpCommand(
                    event.userId(),
                    XpSourceType.CHALLENGE_COMPLETED,
                    event.challengeId().toString(),
                    CHALLENGE_COMPLETED_XP
            ));
        } catch (Exception e) {
            log.error("Failed to award challenge XP for challengeId={}: {}", event.challengeId(), e.getMessage());
        }
    }
}
