package com.studywithus.subscription.domain;

import java.util.UUID;

public record SubscriptionCanceledEvent(
        UUID subscriptionId,
        UUID userId
) {
}
