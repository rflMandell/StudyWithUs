package com.studywithus.notification.infrastructure;

import com.studywithus.notification.domain.DeviceToken;
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
@Table(name = "device_tokens")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceTokenJpaEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 500)
    private String token;

    @Column(nullable = false, length = 20)
    private String platform;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    public static DeviceTokenJpaEntity fromDomain(DeviceToken domain) {
        return DeviceTokenJpaEntity.builder()
                .id(domain.getId())
                .userId(domain.getUserId())
                .token(domain.getToken())
                .platform(domain.getPlatform())
                .active(domain.isActive())
                .createdAt(domain.getCreatedAt() != null ? domain.getCreatedAt() : Instant.now())
                .lastSeenAt(domain.getLastSeenAt() != null ? domain.getLastSeenAt() : Instant.now())
                .build();
    }

    public DeviceToken toDomain() {
        return DeviceToken.builder()
                .id(this.id)
                .userId(this.userId)
                .token(this.token)
                .platform(this.platform)
                .active(this.active)
                .createdAt(this.createdAt)
                .lastSeenAt(this.lastSeenAt)
                .build();
    }
}
