package com.studywithus.moderation.domain;

import com.studywithus.shared.domain.PagedResult;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AuditLogRepository {
    AuditLog save(AuditLog auditLog);
    PagedResult<AuditLog> findAll(String targetType, int page, int size);
    PagedResult<AuditLog> findByFilters(
            UUID actorId,
            String action,
            String targetType,
            UUID targetId,
            Instant from,
            Instant to,
            int page,
            int size
    );
    List<AuditLog> findForExport(
            UUID actorId,
            String action,
            String targetType,
            UUID targetId,
            Instant from,
            Instant to,
            int limit
    );
}
