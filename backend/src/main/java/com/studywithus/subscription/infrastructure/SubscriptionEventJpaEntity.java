package com.studywithus.subscription.infrastructure;

import com.studywithus.subscription.domain.SubscriptionEvent;
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
@Table(name = "subscription_events")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionEventJpaEntity {

    @Id
    private UUID id;

    @Column(name = "subscription_id")
    private UUID subscriptionId;

    @Column(name = "provider_event_id", nullable = false, unique = true)
    private String providerEventId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public SubscriptionEvent toDomain() {
        return SubscriptionEvent.builder()
                .id(this.id)
                .subscriptionId(this.subscriptionId)
                .providerEventId(this.providerEventId)
                .eventType(this.eventType)
                .payload(this.payload)
                .createdAt(this.createdAt)
                .build();
    }

    public static SubscriptionEventJpaEntity fromDomain(SubscriptionEvent domain) {
        return SubscriptionEventJpaEntity.builder()
                .id(domain.getId())
                .subscriptionId(domain.getSubscriptionId())
                .providerEventId(domain.getProviderEventId())
                .eventType(domain.getEventType())
                .payload(domain.getPayload())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
