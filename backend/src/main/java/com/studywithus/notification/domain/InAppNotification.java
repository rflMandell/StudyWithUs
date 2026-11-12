package com.studywithus.notification.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InAppNotification {
    private UUID id;
    private UUID userId;
    private NotificationType type;
    private String title;
    private String body;
    private String referenceType;
    private UUID referenceId;
    private Instant readAt;
    private Instant createdAt;

    public static InAppNotification create(
            UUID userId,
            NotificationType type,
            String title,
            String body,
            String referenceType,
            UUID referenceId
    ) {
        return InAppNotification.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .type(type)
                .title(title)
                .body(body)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .readAt(null)
                .createdAt(Instant.now())
                .build();
    }

    public boolean isRead() {
        return readAt != null;
    }

    public void markAsRead() {
        if (!isRead()) {
            this.readAt = Instant.now();
        }
    }
}
