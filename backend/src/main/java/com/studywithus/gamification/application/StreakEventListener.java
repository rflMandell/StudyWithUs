package com.studywithus.gamification.application;

import com.studywithus.checkin.domain.CheckinCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StreakEventListener {

    private final RecordActivityStreakUseCase recordActivityStreakUseCase;

    @EventListener
    public void onCheckinCreated(CheckinCreatedEvent event) {
        log.info("StreakEventListener received CheckinCreatedEvent for userId: {}, communityId: {}",
                event.userId(), event.communityId());
        try {
            recordActivityStreakUseCase.execute(
                    event.userId(),
                    event.communityId(),
                    event.occurredAt()
            );
        } catch (Exception e) {
            log.error("Failed to update streak for userId: {}, communityId: {}", event.userId(), event.communityId(), e);
        }
    }
}
