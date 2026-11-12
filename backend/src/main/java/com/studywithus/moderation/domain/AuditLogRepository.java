package com.studywithus.moderation.domain;

import com.studywithus.shared.domain.PagedResult;

import java.util.UUID;

public interface AuditLogRepository {
    AuditLog save(AuditLog auditLog);
    PagedResult<AuditLog> findAll(String targetType, int page, int size);
}
