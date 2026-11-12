package com.studywithus.subscription.application;

import com.studywithus.subscription.domain.SubscriptionPlan;

import java.util.UUID;

public record InitiateSubscriptionResult(
        UUID subscriptionId,
        String sessionId,
        String checkoutUrl,
        SubscriptionPlan plan
) {
}
