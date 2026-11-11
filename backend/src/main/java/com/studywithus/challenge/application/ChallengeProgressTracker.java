package com.studywithus.challenge.application;

import com.studywithus.checkin.domain.CheckinCreatedEvent;
import com.studywithus.challenge.domain.Challenge;
import com.studywithus.challenge.domain.ChallengeMetric;
import com.studywithus.challenge.domain.ChallengeParticipant;
import com.studywithus.challenge.domain.ChallengeProgressUpdatedEvent;
import com.studywithus.challenge.domain.ChallengeRepository;
import com.studywithus.challenge.domain.ChallengeReward;
import com.studywithus.challenge.domain.RewardType;
import com.studywithus.gamification.application.AwardPointsCommand;
import com.studywithus.gamification.application.AwardPointsUseCase;
import com.studywithus.gamification.domain.ChallengeCompletedEvent;
import com.studywithus.gamification.domain.PointTransactionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChallengeProgressTracker {

    private final ChallengeRepository challengeRepository;
    private final AwardPointsUseCase awardPointsUseCase;
    private final ApplicationEventPublisher eventPublisher;

    @EventListener
    @Transactional
    public void onCheckinCreated(CheckinCreatedEvent event) {
        if (event.communityId() == null || event.userId() == null) {
            return;
        }

        Instant now = Instant.now();
        List<ChallengeParticipant> participations = challengeRepository.findActiveParticipationsByUserAndCommunity(
                event.userId(),
                event.communityId(),
                now
        );

        if (participations.isEmpty()) {
            return;
        }

        log.debug("Processing checkin event for user {} with {} active challenge participations",
                event.userId(), participations.size());

        for (ChallengeParticipant participant : participations) {
            challengeRepository.findById(participant.getChallengeId()).ifPresent(challenge -> {
                if (challenge.getMetric() == ChallengeMetric.CHECKINS) {
                    processParticipantProgress(challenge, participant, event, now);
                }
            });
        }
    }

    private void processParticipantProgress(
            Challenge challenge,
            ChallengeParticipant participant,
            CheckinCreatedEvent event,
            Instant now
    ) {
        int oldProgress = participant.getProgress();
        boolean justCompleted = participant.incrementProgress(challenge.getTargetValue());

        challengeRepository.saveParticipant(participant);

        eventPublisher.publishEvent(new ChallengeProgressUpdatedEvent(
                challenge.getId(),
                participant.getUserId(),
                oldProgress,
                participant.getProgress(),
                challenge.getTargetValue()
        ));

        if (justCompleted) {
            log.info("User {} successfully completed challenge {} ('{}')!",
                    participant.getUserId(), challenge.getId(), challenge.getTitle());

            distributeRewards(challenge, participant, event, now);
        }
    }

    private void distributeRewards(
            Challenge challenge,
            ChallengeParticipant participant,
            CheckinCreatedEvent event,
            Instant now
    ) {
        if (challenge.getRewards() == null || challenge.getRewards().isEmpty()) {
            return;
        }

        for (ChallengeReward reward : challenge.getRewards()) {
            if (reward.getType() == RewardType.POINTS && reward.getValue() != null && reward.getValue() > 0) {
                log.info("Awarding {} bonus points to user {} for completing challenge {}",
                        reward.getValue(), participant.getUserId(), challenge.getId());

                awardPointsUseCase.execute(new AwardPointsCommand(
                        participant.getUserId(),
                        challenge.getCommunityId(),
                        event.checkinId(),
                        challenge.getId(),
                        reward.getValue(),
                        PointTransactionType.CHALLENGE
                ));
            } else if (reward.getType() == RewardType.BADGE && reward.getBadgeId() != null) {
                log.info("Awarding badge {} to user {} for completing challenge {}",
                        reward.getBadgeId(), participant.getUserId(), challenge.getId());

                eventPublisher.publishEvent(new ChallengeCompletedEvent(
                        challenge.getId(),
                        participant.getUserId(),
                        reward.getBadgeId(),
                        now
                ));
            }
        }
    }
}
