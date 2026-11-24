package com.studywithus.admin.presentation;

import com.studywithus.moderation.domain.AuditLog;

import java.time.Instant;
import java.util.UUID;

public record AdminAuditLogResponse(
        UUID id,
        UUID actorId,
        String actorRole,
        String action,
        String targetType,
        UUID targetId,
        String reason,
        String beforeState,
        String afterState,
        String ipAddress,
        String userAgent,
        String requestId,
        String metadata,
        Instant createdAt
) {
    public static AdminAuditLogResponse from(AuditLog log) {
        return new AdminAuditLogResponse(
                log.getId(),
                log.getActorId(),
                log.getActorRole(),
                log.getAction(),
                log.getTargetType(),
                log.getTargetId(),
                log.getReason(),
                log.getBeforeState(),
                log.getAfterState(),
                log.getIpAddress(),
                log.getUserAgent(),
                log.getRequestId(),
                log.getMetadata(),
                log.getCreatedAt()
        );
    }
}
