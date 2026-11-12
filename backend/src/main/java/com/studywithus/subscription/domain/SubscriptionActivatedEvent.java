package com.studywithus.subscription.domain;

import java.time.Instant;
import java.util.UUID;

public record SubscriptionActivatedEvent(
        UUID subscriptionId,
        UUID userId,
        SubscriptionPlan plan,
        Instant expiresAt
) {
}
