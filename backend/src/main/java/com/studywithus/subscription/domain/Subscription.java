package com.studywithus.subscription.domain;

import com.studywithus.shared.domain.DomainException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Subscription {

    private UUID id;
    private UUID userId;
    private SubscriptionProvider provider;
    private String externalId;
    private SubscriptionPlan plan;
    private SubscriptionStatus status;
    private Instant startedAt;
    private Instant expiresAt;
    private Instant createdAt;
    private Instant updatedAt;

    public static Subscription createPending(
            UUID userId,
            SubscriptionProvider provider,
            String externalId,
            SubscriptionPlan plan
    ) {
        if (userId == null) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "INVALID_USER_ID", "User ID is required");
        }
        if (provider == null) {
            provider = SubscriptionProvider.MOCK;
        }
        if (plan == null) {
            plan = SubscriptionPlan.PREMIUM;
        }

        Instant now = Instant.now();
        return Subscription.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .provider(provider)
                .externalId(externalId != null ? externalId : "sub_" + UUID.randomUUID())
                .plan(plan)
                .status(SubscriptionStatus.PENDING)
                .startedAt(now)
                .expiresAt(null)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public void activate(Instant expiresAt) {
        this.status = SubscriptionStatus.ACTIVE;
        this.startedAt = Instant.now();
        this.expiresAt = expiresAt;
        this.updatedAt = Instant.now();
    }

    public void cancel() {
        this.status = SubscriptionStatus.CANCELED;
        this.updatedAt = Instant.now();
    }

    public void expire() {
        this.status = SubscriptionStatus.EXPIRED;
        this.updatedAt = Instant.now();
    }

    public void renew(Instant newExpiresAt) {
        this.status = SubscriptionStatus.ACTIVE;
        this.expiresAt = newExpiresAt;
        this.updatedAt = Instant.now();
    }

    public boolean isActive() {
        if (this.status != SubscriptionStatus.ACTIVE) {
            return false;
        }
        return this.expiresAt == null || this.expiresAt.isAfter(Instant.now());
    }
}
