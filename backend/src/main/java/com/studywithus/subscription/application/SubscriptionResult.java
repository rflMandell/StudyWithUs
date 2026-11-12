package com.studywithus.subscription.application;

import com.studywithus.subscription.domain.Subscription;
import com.studywithus.subscription.domain.SubscriptionPlan;
import com.studywithus.subscription.domain.SubscriptionProvider;
import com.studywithus.subscription.domain.SubscriptionStatus;

import java.time.Instant;
import java.util.UUID;

public record SubscriptionResult(
        UUID id,
        UUID userId,
        SubscriptionProvider provider,
        String externalId,
        SubscriptionPlan plan,
        SubscriptionStatus status,
        boolean active,
        Instant startedAt,
        Instant expiresAt,
        Instant createdAt,
        Instant updatedAt
) {
    public static SubscriptionResult fromDomain(Subscription domain) {
        return new SubscriptionResult(
                domain.getId(),
                domain.getUserId(),
                domain.getProvider(),
                domain.getExternalId(),
                domain.getPlan(),
                domain.getStatus(),
                domain.isActive(),
                domain.getStartedAt(),
                domain.getExpiresAt(),
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
    }

    public static SubscriptionResult free(UUID userId) {
        return new SubscriptionResult(
                null,
                userId,
                SubscriptionProvider.MOCK,
                null,
                SubscriptionPlan.FREE,
                SubscriptionStatus.ACTIVE,
                true,
                Instant.EPOCH,
                null,
                Instant.EPOCH,
                Instant.EPOCH
        );
    }
}
