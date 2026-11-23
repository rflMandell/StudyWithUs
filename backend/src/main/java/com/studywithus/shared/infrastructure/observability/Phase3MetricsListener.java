package com.studywithus.shared.infrastructure.observability;

import com.studywithus.checkin.domain.StudyActivityRecordedEvent;
import com.studywithus.gamification.domain.UserLevelUpEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class Phase3MetricsListener {

    private final Counter studyActivitiesRecordedCounter;
    private final Counter userLevelUpsObservedCounter;

    public Phase3MetricsListener(MeterRegistry meterRegistry) {
        this.studyActivitiesRecordedCounter = Counter.builder("studywithus_analytics_events_recorded_total")
                .description("Total number of study activity events recorded for analytics")
                .register(meterRegistry);
        this.userLevelUpsObservedCounter = Counter.builder("studywithus_phase3_level_ups_observed_total")
                .description("Total number of user level-ups observed in Phase 3")
                .register(meterRegistry);
    }

    @EventListener
    public void onStudyActivityRecorded(StudyActivityRecordedEvent event) {
        studyActivitiesRecordedCounter.increment();
        log.debug("Metric incremented: studywithus_analytics_events_recorded_total (checkinId={})", event.checkinId());
    }

    @EventListener
    public void onUserLevelUp(UserLevelUpEvent event) {
        userLevelUpsObservedCounter.increment();
        log.info("Metric incremented: studywithus_phase3_level_ups_observed_total (userId={}, newLevel={})",
                event.userId(), event.newLevel());
    }
}
