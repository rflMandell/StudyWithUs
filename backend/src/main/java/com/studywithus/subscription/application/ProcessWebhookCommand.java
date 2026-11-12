package com.studywithus.subscription.application;

public record ProcessWebhookCommand(
        String signatureHeader,
        String rawPayload
) {
}
