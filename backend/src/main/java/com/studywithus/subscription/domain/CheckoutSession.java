package com.studywithus.subscription.domain;

public record CheckoutSession(
        String sessionId,
        String checkoutUrl
) {
}
