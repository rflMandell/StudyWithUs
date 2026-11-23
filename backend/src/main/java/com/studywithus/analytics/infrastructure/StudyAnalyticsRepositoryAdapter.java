package com.studywithus.analytics.infrastructure;

import com.studywithus.analytics.domain.StudyActivityRecord;
import com.studywithus.analytics.domain.StudyAnalyticsRepository;
import com.studywithus.analytics.domain.StudyDailyAggregate;
import com.studywithus.analytics.domain.StudySubjectAggregate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class StudyAnalyticsRepositoryAdapter implements StudyAnalyticsRepository {

    private final SpringDataStudyActivityRecordRepository recordRepo;
    private final SpringDataStudyDailyAggregateRepository dailyRepo;
    private final SpringDataStudySubjectAggregateRepository subjectRepo;

    @Override
    public boolean existsRecordByCheckinId(UUID checkinId) {
        return recordRepo.existsById(checkinId);
    }

    @Override
    public Optional<StudyActivityRecord> findRecordByCheckinId(UUID checkinId) {
        return recordRepo.findById(checkinId).map(StudyActivityRecordJpaEntity::toDomain);
    }

    @Override
    public StudyActivityRecord saveRecord(StudyActivityRecord record) {
        return recordRepo.save(StudyActivityRecordJpaEntity.fromDomain(record)).toDomain();
    }

    @Override
    public void deleteRecordByCheckinId(UUID checkinId) {
        recordRepo.deleteById(checkinId);
    }

    @Override
    public Optional<StudyDailyAggregate> findDailyAggregate(UUID userId, LocalDate activityDate) {
        return dailyRepo.findByUserIdAndActivityDate(userId, activityDate).map(StudyDailyAggregateJpaEntity::toDomain);
    }

    @Override
    public StudyDailyAggregate saveDailyAggregate(StudyDailyAggregate aggregate) {
        return dailyRepo.save(StudyDailyAggregateJpaEntity.fromDomain(aggregate)).toDomain();
    }

    @Override
    public List<StudyDailyAggregate> findDailyAggregatesBetween(UUID userId, LocalDate startInclusive, LocalDate endInclusive) {
        return dailyRepo.findByUserIdAndActivityDateBetweenOrderByActivityDateAsc(userId, startInclusive, endInclusive)
                .stream()
                .map(StudyDailyAggregateJpaEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<StudySubjectAggregate> findSubjectAggregate(UUID userId, String subject) {
        return subjectRepo.findByUserIdAndSubject(userId, subject).map(StudySubjectAggregateJpaEntity::toDomain);
    }

    @Override
    public StudySubjectAggregate saveSubjectAggregate(StudySubjectAggregate aggregate) {
        return subjectRepo.save(StudySubjectAggregateJpaEntity.fromDomain(aggregate)).toDomain();
    }

    @Override
    public List<StudySubjectAggregate> findSubjectAggregatesByUserId(UUID userId) {
        return subjectRepo.findByUserIdOrderByTotalMinutesDescSessionsCountDesc(userId)
                .stream()
                .map(StudySubjectAggregateJpaEntity::toDomain)
                .toList();
    }
}
