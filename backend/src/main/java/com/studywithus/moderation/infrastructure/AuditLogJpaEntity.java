package com.studywithus.moderation.infrastructure;

import com.studywithus.moderation.domain.AuditLog;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Immutable
@Table(name = "audit_logs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogJpaEntity {

    @Id
    private UUID id;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(name = "actor_role", length = 30)
    private String actorRole;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(name = "target_type", nullable = false, length = 50)
    private String targetType;

    @Column(name = "target_id")
    private UUID targetId;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "before_state")
    private String beforeState;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "after_state")
    private String afterState;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "request_id", length = 64)
    private String requestId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata")
    private String metadata;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static AuditLogJpaEntity fromDomain(AuditLog domain) {
        return AuditLogJpaEntity.builder()
                .id(domain.getId())
                .actorId(domain.getActorId())
                .actorRole(domain.getActorRole())
                .action(domain.getAction())
                .targetType(domain.getTargetType())
                .targetId(domain.getTargetId())
                .reason(domain.getReason())
                .beforeState(domain.getBeforeState())
                .afterState(domain.getAfterState())
                .ipAddress(domain.getIpAddress())
                .userAgent(domain.getUserAgent())
                .requestId(domain.getRequestId())
                .metadata(domain.getMetadata())
                .createdAt(domain.getCreatedAt() != null ? domain.getCreatedAt() : Instant.now())
                .build();
    }

    public AuditLog toDomain() {
        return AuditLog.builder()
                .id(id)
                .actorId(actorId)
                .actorRole(actorRole)
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .reason(reason)
                .beforeState(beforeState)
                .afterState(afterState)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .requestId(requestId)
                .metadata(metadata)
                .createdAt(createdAt)
                .build();
    }
}
