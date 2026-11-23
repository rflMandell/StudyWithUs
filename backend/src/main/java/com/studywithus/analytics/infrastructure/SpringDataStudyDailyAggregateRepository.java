package com.studywithus.analytics.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataStudyDailyAggregateRepository extends JpaRepository<StudyDailyAggregateJpaEntity, UUID> {
    Optional<StudyDailyAggregateJpaEntity> findByUserIdAndActivityDate(UUID userId, LocalDate activityDate);
    List<StudyDailyAggregateJpaEntity> findByUserIdAndActivityDateBetweenOrderByActivityDateAsc(
            UUID userId,
            LocalDate startInclusive,
            LocalDate endInclusive
    );
}
