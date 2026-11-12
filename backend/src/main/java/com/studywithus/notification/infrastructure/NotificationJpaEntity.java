package com.studywithus.notification.infrastructure;

import com.studywithus.notification.domain.InAppNotification;
import com.studywithus.notification.domain.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "notifications")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationJpaEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotificationType type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(name = "reference_type", length = 50)
    private String referenceType;

    @Column(name = "reference_id")
    private UUID referenceId;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public static NotificationJpaEntity fromDomain(InAppNotification domain) {
        return NotificationJpaEntity.builder()
                .id(domain.getId())
                .userId(domain.getUserId())
                .type(domain.getType())
                .title(domain.getTitle())
                .body(domain.getBody())
                .referenceType(domain.getReferenceType())
                .referenceId(domain.getReferenceId())
                .readAt(domain.getReadAt())
                .createdAt(domain.getCreatedAt() != null ? domain.getCreatedAt() : Instant.now())
                .build();
    }

    public InAppNotification toDomain() {
        return InAppNotification.builder()
                .id(this.id)
                .userId(this.userId)
                .type(this.type)
                .title(this.title)
                .body(this.body)
                .referenceType(this.referenceType)
                .referenceId(this.referenceId)
                .readAt(this.readAt)
                .createdAt(this.createdAt)
                .build();
    }
}
