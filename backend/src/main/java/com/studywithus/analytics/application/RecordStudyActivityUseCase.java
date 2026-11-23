package com.studywithus.analytics.application;

import com.studywithus.analytics.domain.StudyActivityRecord;
import com.studywithus.analytics.domain.StudyAnalyticsRepository;
import com.studywithus.analytics.domain.StudyDailyAggregate;
import com.studywithus.analytics.domain.StudySubjectAggregate;
import com.studywithus.checkin.domain.StudyActivityRecordedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecordStudyActivityUseCase {

    private final StudyAnalyticsRepository repository;

    @Transactional
    public void record(StudyActivityRecordedEvent event) {
        if (repository.existsRecordByCheckinId(event.checkinId())) {
            return;
        }

        LocalDate activityDate = LocalDate.ofInstant(event.occurredAt(), ZoneOffset.UTC);
        String subject = resolveSubject(event);
        int durationMinutes = Math.max(event.durationMinutes(), 0);

        repository.saveRecord(StudyActivityRecord.create(
                event.checkinId(),
                event.userId(),
                activityDate,
                subject,
                durationMinutes
        ));

        StudyDailyAggregate daily = repository.findDailyAggregate(event.userId(), activityDate)
                .orElseGet(() -> StudyDailyAggregate.create(event.userId(), activityDate));
        daily.addSession(durationMinutes);
        repository.saveDailyAggregate(daily);

        StudySubjectAggregate subjectAgg = repository.findSubjectAggregate(event.userId(), subject)
                .orElseGet(() -> StudySubjectAggregate.create(event.userId(), subject));
        subjectAgg.addSession(durationMinutes);
        repository.saveSubjectAggregate(subjectAgg);

        log.debug("Recorded study analytics for checkinId={}, userId={}, date={}, subject={}, minutes={}",
                event.checkinId(), event.userId(), activityDate, subject, durationMinutes);
    }

    @Transactional
    public void reverse(UUID checkinId) {
        repository.findRecordByCheckinId(checkinId).ifPresent(record -> {
            repository.findDailyAggregate(record.getUserId(), record.getActivityDate()).ifPresent(daily -> {
                daily.removeSession(record.getDurationMinutes());
                repository.saveDailyAggregate(daily);
            });
            repository.findSubjectAggregate(record.getUserId(), record.getSubject()).ifPresent(subjectAgg -> {
                subjectAgg.removeSession(record.getDurationMinutes());
                repository.saveSubjectAggregate(subjectAgg);
            });
            repository.deleteRecordByCheckinId(checkinId);
            log.debug("Reversed study analytics for deleted checkinId={}", checkinId);
        });
    }

    private String resolveSubject(StudyActivityRecordedEvent event) {
        if (event.subject() != null && !event.subject().isBlank()) {
            return event.subject().trim();
        }
        if (event.tags() != null && !event.tags().isEmpty()) {
            String firstTag = event.tags().get(0);
            if (firstTag != null && !firstTag.isBlank()) {
                return firstTag.trim();
            }
        }
        return "Geral";
    }
}
