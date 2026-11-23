package com.studywithus.league.application;

import com.studywithus.checkin.domain.Checkin;
import com.studywithus.checkin.domain.CheckinCreatedEvent;
import com.studywithus.checkin.domain.CheckinDeletedEvent;
import com.studywithus.checkin.domain.CheckinRepository;
import com.studywithus.gamification.domain.ChallengeCompletedEvent;
import com.studywithus.gamification.domain.StreakUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LeagueEventListener {

    public static final long BASE_CHECKIN_SCORE = 20L;
    public static final long QUALITY_CAPTION_BONUS = 10L;
    public static final long QUALITY_MEDIA_BONUS = 10L;
    public static final long STREAK_BONUS_SCORE = 15L;
    public static final long CHALLENGE_BONUS_SCORE = 50L;

    private final RecordLeagueScoreUseCase recordLeagueScoreUseCase;
    private final CheckinRepository checkinRepository;

    @EventListener
    public void onCheckinCreated(CheckinCreatedEvent event) {
        try {
            long score = BASE_CHECKIN_SCORE;
            Checkin checkin = checkinRepository.findById(event.checkinId()).orElse(null);
            if (checkin != null) {
                if (checkin.getCaption() != null && checkin.getCaption().trim().length() >= 20) {
                    score += QUALITY_CAPTION_BONUS;
                }
                if (checkin.getMedia() != null && !checkin.getMedia().isEmpty()) {
                    score += QUALITY_MEDIA_BONUS;
                }
            }
            recordLeagueScoreUseCase.execute(event.userId(), score);
        } catch (Exception e) {
            log.warn("Failed to record league score for checkinId={}: {}", event.checkinId(), e.getMessage());
        }
    }

    @EventListener
    public void onCheckinDeleted(CheckinDeletedEvent event) {
        try {
            recordLeagueScoreUseCase.execute(event.userId(), -BASE_CHECKIN_SCORE);
        } catch (Exception e) {
            log.warn("Failed to reverse league score for checkinId={}: {}", event.checkinId(), e.getMessage());
        }
    }

    @EventListener
    public void onStreakUpdated(StreakUpdatedEvent event) {
        if (event.userCurrentStreak() < 2) {
            return;
        }
        try {
            recordLeagueScoreUseCase.execute(event.userId(), STREAK_BONUS_SCORE);
        } catch (Exception e) {
            log.warn("Failed to record streak league score for userId={}: {}", event.userId(), e.getMessage());
        }
    }

    @EventListener
    public void onChallengeCompleted(ChallengeCompletedEvent event) {
        try {
            recordLeagueScoreUseCase.execute(event.userId(), CHALLENGE_BONUS_SCORE);
        } catch (Exception e) {
            log.warn("Failed to record challenge league score for challengeId={}: {}", event.challengeId(), e.getMessage());
        }
    }
}
