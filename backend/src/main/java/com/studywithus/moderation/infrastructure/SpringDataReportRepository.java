package com.studywithus.moderation.infrastructure;

import com.studywithus.moderation.domain.ReportStatus;
import com.studywithus.moderation.domain.ReportTargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataReportRepository extends JpaRepository<ReportJpaEntity, UUID> {

    Page<ReportJpaEntity> findByStatusOrderByCreatedAtDesc(ReportStatus status, Pageable pageable);

    Page<ReportJpaEntity> findByStatusAndTargetTypeOrderByCreatedAtDesc(ReportStatus status, ReportTargetType targetType, Pageable pageable);

    Page<ReportJpaEntity> findByTargetTypeOrderByCreatedAtDesc(ReportTargetType targetType, Pageable pageable);

    Page<ReportJpaEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByStatus(ReportStatus status);
}
