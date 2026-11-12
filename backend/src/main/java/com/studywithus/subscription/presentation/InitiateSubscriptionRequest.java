package com.studywithus.subscription.presentation;

import com.studywithus.subscription.domain.SubscriptionPlan;

public record InitiateSubscriptionRequest(
        SubscriptionPlan plan,
        String returnUrl
) {
}
