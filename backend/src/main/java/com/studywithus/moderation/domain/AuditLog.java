package com.studywithus.moderation.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {
    private UUID id;
    private UUID actorId;
    private String action;
    private String targetType;
    private UUID targetId;
    private String metadata;
    private Instant createdAt;

    public static AuditLog create(
            UUID actorId,
            String action,
            String targetType,
            UUID targetId,
            String metadata
    ) {
        return AuditLog.builder()
                .id(UUID.randomUUID())
                .actorId(actorId)
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .metadata(metadata)
                .createdAt(Instant.now())
                .build();
    }
}
