package com.studywithus.moderation.infrastructure;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataAuditLogRepository extends JpaRepository<AuditLogJpaEntity, UUID> {

    Page<AuditLogJpaEntity> findByTargetTypeOrderByCreatedAtDesc(String targetType, Pageable pageable);

    Page<AuditLogJpaEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
