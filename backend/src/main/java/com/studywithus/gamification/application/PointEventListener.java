package com.studywithus.gamification.application;

import com.studywithus.checkin.domain.CheckinCreatedEvent;
import com.studywithus.checkin.domain.CheckinDeletedEvent;
import com.studywithus.gamification.domain.PointTransaction;
import com.studywithus.gamification.domain.PointTransactionRepository;
import com.studywithus.gamification.domain.PointTransactionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PointEventListener {

    public static final int DEFAULT_CHECKIN_POINTS = 10;

    private final AwardPointsUseCase awardPointsUseCase;
    private final ReversePointsUseCase reversePointsUseCase;
    private final PointTransactionRepository pointTransactionRepository;

    @EventListener
    public void onCheckinCreated(CheckinCreatedEvent event) {
        log.info("CheckinCreatedEvent received for checkinId: {}, userId: {}", event.checkinId(), event.userId());
        try {
            awardPointsUseCase.execute(new AwardPointsCommand(
                    event.userId(),
                    event.communityId(),
                    event.checkinId(),
                    null,
                    DEFAULT_CHECKIN_POINTS,
                    PointTransactionType.CHECKIN
            ));
        } catch (Exception e) {
            log.error("Failed to award points for checkinId: {}", event.checkinId(), e);
        }
    }

    @EventListener
    public void onCheckinDeleted(CheckinDeletedEvent event) {
        log.info("CheckinDeletedEvent received for checkinId: {}, userId: {}", event.checkinId(), event.userId());
        try {
            Optional<PointTransaction> originalTx = pointTransactionRepository.findByCheckinIdAndType(
                    event.checkinId(),
                    PointTransactionType.CHECKIN
            );

            if (originalTx.isPresent() && !originalTx.get().isReversed()) {
                reversePointsUseCase.execute(new ReversePointsCommand(originalTx.get().getId()));
            }
        } catch (Exception e) {
            log.error("Failed to reverse points for checkinId: {}", event.checkinId(), e);
        }
    }
}
