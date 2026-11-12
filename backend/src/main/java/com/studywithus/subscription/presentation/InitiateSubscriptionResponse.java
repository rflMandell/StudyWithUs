package com.studywithus.subscription.presentation;

import com.studywithus.subscription.application.InitiateSubscriptionResult;
import com.studywithus.subscription.domain.SubscriptionPlan;

import java.util.UUID;

public record InitiateSubscriptionResponse(
        UUID subscriptionId,
        String sessionId,
        String checkoutUrl,
        SubscriptionPlan plan
) {
    public static InitiateSubscriptionResponse from(InitiateSubscriptionResult result) {
        return new InitiateSubscriptionResponse(
                result.subscriptionId(),
                result.sessionId(),
                result.checkoutUrl(),
                result.plan()
        );
    }
}
