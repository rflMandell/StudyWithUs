package com.studywithus.moderation.presentation;

import com.studywithus.moderation.application.AuditLogResult;

import java.time.Instant;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        UUID actorId,
        String action,
        String targetType,
        UUID targetId,
        String metadata,
        Instant createdAt
) {
    public static AuditLogResponse from(AuditLogResult result) {
        return new AuditLogResponse(
                result.id(),
                result.actorId(),
                result.action(),
                result.targetType(),
                result.targetId(),
                result.metadata(),
                result.createdAt()
        );
    }
}
