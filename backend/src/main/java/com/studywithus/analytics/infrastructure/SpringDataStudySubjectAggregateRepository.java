package com.studywithus.analytics.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataStudySubjectAggregateRepository extends JpaRepository<StudySubjectAggregateJpaEntity, UUID> {
    Optional<StudySubjectAggregateJpaEntity> findByUserIdAndSubject(UUID userId, String subject);
    List<StudySubjectAggregateJpaEntity> findByUserIdOrderByTotalMinutesDescSessionsCountDesc(UUID userId);
}
