package com.studywithus.subscription.domain;

import com.studywithus.shared.domain.DomainException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionEvent {

    private UUID id;
    private UUID subscriptionId;
    private String providerEventId;
    private String eventType;
    private String payload;
    private Instant createdAt;

    public static SubscriptionEvent create(
            UUID subscriptionId,
            String providerEventId,
            String eventType,
            String payload
    ) {
        if (providerEventId == null || providerEventId.isBlank()) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "INVALID_EVENT_ID", "Provider event ID is required");
        }
        if (eventType == null || eventType.isBlank()) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "INVALID_EVENT_TYPE", "Event type is required");
        }

        return SubscriptionEvent.builder()
                .id(UUID.randomUUID())
                .subscriptionId(subscriptionId)
                .providerEventId(providerEventId.trim())
                .eventType(eventType.trim())
                .payload(payload != null ? payload : "{}")
                .createdAt(Instant.now())
                .build();
    }
}
