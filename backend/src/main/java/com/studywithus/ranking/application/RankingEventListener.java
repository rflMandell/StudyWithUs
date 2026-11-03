package com.studywithus.ranking.application;

import com.studywithus.gamification.domain.PointsAwardedEvent;
import com.studywithus.gamification.domain.PointsReversedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RankingEventListener {

    private final UpdateCommunityRankingUseCase updateCommunityRankingUseCase;

    @EventListener
    public void onPointsAwarded(PointsAwardedEvent event) {
        if (event.communityId() != null) {
            log.info("PointsAwardedEvent received for communityId: {}, userId: {}, amount: {}", event.communityId(), event.userId(), event.amount());
            try {
                updateCommunityRankingUseCase.execute(event.communityId(), event.userId(), event.amount());
            } catch (Exception e) {
                log.error("Failed to update ranking on points awarded: {}", event, e);
            }
        }
    }

    @EventListener
    public void onPointsReversed(PointsReversedEvent event) {
        if (event.communityId() != null) {
            log.info("PointsReversedEvent received for communityId: {}, userId: {}, amount: {}", event.communityId(), event.userId(), event.amount());
            try {
                updateCommunityRankingUseCase.execute(event.communityId(), event.userId(), event.amount());
            } catch (Exception e) {
                log.error("Failed to update ranking on points reversed: {}", event, e);
            }
        }
    }
}
