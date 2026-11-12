package com.studywithus.subscription.application;

import com.studywithus.subscription.domain.SubscriptionPlan;

import java.util.UUID;

public record InitiateSubscriptionCommand(
        UUID userId,
        SubscriptionPlan plan,
        String returnUrl
) {
}
