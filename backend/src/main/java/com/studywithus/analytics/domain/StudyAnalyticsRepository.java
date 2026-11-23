package com.studywithus.analytics.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudyAnalyticsRepository {
    boolean existsRecordByCheckinId(UUID checkinId);
    Optional<StudyActivityRecord> findRecordByCheckinId(UUID checkinId);
    StudyActivityRecord saveRecord(StudyActivityRecord record);
    void deleteRecordByCheckinId(UUID checkinId);

    Optional<StudyDailyAggregate> findDailyAggregate(UUID userId, LocalDate activityDate);
    StudyDailyAggregate saveDailyAggregate(StudyDailyAggregate aggregate);
    List<StudyDailyAggregate> findDailyAggregatesBetween(UUID userId, LocalDate startInclusive, LocalDate endInclusive);

    Optional<StudySubjectAggregate> findSubjectAggregate(UUID userId, String subject);
    StudySubjectAggregate saveSubjectAggregate(StudySubjectAggregate aggregate);
    List<StudySubjectAggregate> findSubjectAggregatesByUserId(UUID userId);
}
