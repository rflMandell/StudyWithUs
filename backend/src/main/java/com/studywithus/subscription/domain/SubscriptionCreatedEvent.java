package com.studywithus.subscription.domain;

import java.util.UUID;

public record SubscriptionCreatedEvent(
        UUID subscriptionId,
        UUID userId,
        SubscriptionPlan plan
) {
}
