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

import java.time.Instant;
import java.util.UUID;

@Entity
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

    @Column(nullable = false, length = 100)
    private String action;

    @Column(name = "target_type", nullable = false, length = 50)
    private String targetType;

    @Column(name = "target_id")
    private UUID targetId;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public static AuditLogJpaEntity fromDomain(AuditLog domain) {
        return AuditLogJpaEntity.builder()
                .id(domain.getId())
                .actorId(domain.getActorId())
                .action(domain.getAction())
                .targetType(domain.getTargetType())
                .targetId(domain.getTargetId())
                .metadata(domain.getMetadata())
                .createdAt(domain.getCreatedAt() != null ? domain.getCreatedAt() : Instant.now())
                .build();
    }

    public AuditLog toDomain() {
        return AuditLog.builder()
                .id(this.id)
                .actorId(this.actorId)
                .action(this.action)
                .targetType(this.targetType)
                .targetId(this.targetId)
                .metadata(this.metadata)
                .createdAt(this.createdAt)
                .build();
    }
}
