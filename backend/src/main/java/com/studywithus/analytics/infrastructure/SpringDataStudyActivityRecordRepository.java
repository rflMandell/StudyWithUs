package com.studywithus.analytics.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataStudyActivityRecordRepository extends JpaRepository<StudyActivityRecordJpaEntity, UUID> {
}
