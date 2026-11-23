package com.studywithus.analytics.application;

import com.studywithus.checkin.domain.CheckinDeletedEvent;
import com.studywithus.checkin.domain.StudyActivityRecordedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class StudyAnalyticsEventListener {

    private final RecordStudyActivityUseCase recordStudyActivityUseCase;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onStudyActivityRecorded(StudyActivityRecordedEvent event) {
        try {
            recordStudyActivityUseCase.record(event);
        } catch (Exception e) {
            log.warn("Failed async study analytics collection for checkinId={}: {}", event.checkinId(), e.getMessage());
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onCheckinDeleted(CheckinDeletedEvent event) {
        try {
            recordStudyActivityUseCase.reverse(event.checkinId());
        } catch (Exception e) {
            log.warn("Failed async study analytics reversal for checkinId={}: {}", event.checkinId(), e.getMessage());
        }
    }
}
