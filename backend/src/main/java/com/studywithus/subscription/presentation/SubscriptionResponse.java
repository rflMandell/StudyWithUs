package com.studywithus.subscription.presentation;

import com.studywithus.subscription.application.SubscriptionResult;
import com.studywithus.subscription.domain.SubscriptionPlan;
import com.studywithus.subscription.domain.SubscriptionProvider;
import com.studywithus.subscription.domain.SubscriptionStatus;

import java.time.Instant;
import java.util.UUID;

public record SubscriptionResponse(
        UUID id,
        UUID userId,
        SubscriptionProvider provider,
        SubscriptionPlan plan,
        SubscriptionStatus status,
        boolean active,
        Instant startedAt,
        Instant expiresAt,
        Instant createdAt
) {
    public static SubscriptionResponse from(SubscriptionResult result) {
        return new SubscriptionResponse(
                result.id(),
                result.userId(),
                result.provider(),
                result.plan(),
                result.status(),
                result.active(),
                result.startedAt(),
                result.expiresAt(),
                result.createdAt()
        );
    }
}
