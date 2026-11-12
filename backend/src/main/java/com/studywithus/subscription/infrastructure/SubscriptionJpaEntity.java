package com.studywithus.subscription.infrastructure;

import com.studywithus.subscription.domain.Subscription;
import com.studywithus.subscription.domain.SubscriptionPlan;
import com.studywithus.subscription.domain.SubscriptionProvider;
import com.studywithus.subscription.domain.SubscriptionStatus;
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
@Table(name = "subscriptions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionJpaEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 50)
    private String provider;

    @Column(name = "external_id", nullable = false)
    private String externalId;

    @Column(nullable = false, length = 50)
    private String plan;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Subscription toDomain() {
        return Subscription.builder()
                .id(this.id)
                .userId(this.userId)
                .provider(SubscriptionProvider.valueOf(this.provider))
                .externalId(this.externalId)
                .plan(SubscriptionPlan.valueOf(this.plan))
                .status(SubscriptionStatus.valueOf(this.status))
                .startedAt(this.startedAt)
                .expiresAt(this.expiresAt)
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt)
                .build();
    }

    public static SubscriptionJpaEntity fromDomain(Subscription domain) {
        return SubscriptionJpaEntity.builder()
                .id(domain.getId())
                .userId(domain.getUserId())
                .provider(domain.getProvider().name())
                .externalId(domain.getExternalId())
                .plan(domain.getPlan().name())
                .status(domain.getStatus().name())
                .startedAt(domain.getStartedAt())
                .expiresAt(domain.getExpiresAt())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}
