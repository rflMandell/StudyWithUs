package com.studywithus.subscription.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studywithus.shared.domain.DomainException;
import com.studywithus.subscription.domain.PaymentProviderPort;
import com.studywithus.subscription.domain.Subscription;
import com.studywithus.subscription.domain.SubscriptionActivatedEvent;
import com.studywithus.subscription.domain.SubscriptionCanceledEvent;
import com.studywithus.subscription.domain.SubscriptionEvent;
import com.studywithus.subscription.domain.SubscriptionEventRepository;
import com.studywithus.subscription.domain.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessWebhookUseCase {

    private final PaymentProviderPort paymentProviderPort;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionEventRepository subscriptionEventRepository;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void execute(ProcessWebhookCommand command) {
        if (!paymentProviderPort.verifyWebhookSignature(command.rawPayload(), command.signatureHeader())) {
            log.warn("Invalid webhook signature received");
            throw new DomainException(HttpStatus.UNAUTHORIZED, "INVALID_WEBHOOK_SIGNATURE", "Webhook signature verification failed");
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(command.rawPayload());
        } catch (Exception e) {
            log.error("Failed to parse webhook JSON payload", e);
            throw new DomainException(HttpStatus.BAD_REQUEST, "INVALID_PAYLOAD", "Payload is not valid JSON");
        }

        String providerEventId = extractField(root, "id", "event_id", "eventId");
        if (providerEventId == null || providerEventId.isBlank()) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "MISSING_EVENT_ID", "Webhook payload missing event ID");
        }

        // Idempotency check
        if (subscriptionEventRepository.existsByProviderEventId(providerEventId)) {
            log.info("Duplicate webhook event ignored: {}", providerEventId);
            return;
        }

        String eventType = extractField(root, "type", "event_type", "eventType");
        if (eventType == null || eventType.isBlank()) {
            eventType = "unknown";
        }

        String externalId = extractExternalId(root);
        Subscription subscription = resolveSubscription(root, externalId);

        SubscriptionEvent event = SubscriptionEvent.create(
                subscription != null ? subscription.getId() : null,
                providerEventId,
                eventType,
                command.rawPayload()
        );
        subscriptionEventRepository.save(event);

        if (subscription != null) {
            applyEventToSubscription(subscription, eventType);
        } else {
            log.warn("Webhook event {} received but no associated subscription found (externalId={})", eventType, externalId);
        }
    }

    private void applyEventToSubscription(Subscription subscription, String eventType) {
        switch (eventType) {
            case "checkout.session.completed", "payment.succeeded", "subscription.activated", "invoice.paid" -> {
                subscription.activate(Instant.now().plus(30, ChronoUnit.DAYS));
                subscriptionRepository.save(subscription);
                eventPublisher.publishEvent(new SubscriptionActivatedEvent(
                        subscription.getId(),
                        subscription.getUserId(),
                        subscription.getPlan(),
                        subscription.getExpiresAt()
                ));
                log.info("Subscription activated: id={}, userId={}, plan={}", subscription.getId(), subscription.getUserId(), subscription.getPlan());
            }
            case "customer.subscription.deleted", "subscription.canceled" -> {
                subscription.cancel();
                subscriptionRepository.save(subscription);
                eventPublisher.publishEvent(new SubscriptionCanceledEvent(subscription.getId(), subscription.getUserId()));
                log.info("Subscription canceled: id={}, userId={}", subscription.getId(), subscription.getUserId());
            }
            case "customer.subscription.expired", "payment.failed" -> {
                subscription.expire();
                subscriptionRepository.save(subscription);
                log.info("Subscription expired: id={}, userId={}", subscription.getId(), subscription.getUserId());
            }
            default -> log.debug("Unhandled subscription webhook event type: {}", eventType);
        }
    }

    private Subscription resolveSubscription(JsonNode root, String externalId) {
        if (externalId != null && !externalId.isBlank()) {
            Optional<Subscription> byExternalId = subscriptionRepository.findByExternalId(externalId);
            if (byExternalId.isPresent()) {
                return byExternalId.get();
            }
        }

        String userIdStr = extractField(root, "userId", "user_id");
        if (userIdStr != null) {
            try {
                UUID userId = UUID.fromString(userIdStr);
                return subscriptionRepository.findByUserId(userId).orElse(null);
            } catch (IllegalArgumentException ignored) {}
        }

        return null;
    }

    private String extractExternalId(JsonNode root) {
        if (root.has("data") && root.get("data").has("object")) {
            JsonNode object = root.get("data").get("object");
            String id = extractField(object, "id", "sessionId", "session_id", "subscription");
            if (id != null) {
                return id;
            }
        }
        return extractField(root, "external_id", "externalId", "sessionId", "session_id");
    }

    private String extractField(JsonNode node, String... fieldNames) {
        for (String field : fieldNames) {
            if (node.hasNonNull(field)) {
                return node.get(field).asText();
            }
        }
        return null;
    }
}
