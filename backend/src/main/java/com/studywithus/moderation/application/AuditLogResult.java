package com.studywithus.moderation.application;

import com.studywithus.moderation.domain.AuditLog;

import java.time.Instant;
import java.util.UUID;

public record AuditLogResult(
        UUID id,
        UUID actorId,
        String action,
        String targetType,
        UUID targetId,
        String metadata,
        Instant createdAt
) {
    public static AuditLogResult fromDomain(AuditLog auditLog) {
        return new AuditLogResult(
                auditLog.getId(),
                auditLog.getActorId(),
                auditLog.getAction(),
                auditLog.getTargetType(),
                auditLog.getTargetId(),
                auditLog.getMetadata(),
                auditLog.getCreatedAt()
        );
    }
}
