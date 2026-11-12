package com.studywithus.subscription.domain;

import java.util.UUID;

public interface PaymentProviderPort {
    CheckoutSession createCheckoutSession(UUID userId, SubscriptionPlan plan, String returnUrl);
    boolean verifyWebhookSignature(String payload, String signatureHeader);
}
