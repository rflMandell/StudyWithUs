package com.studywithus.notification.domain;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record RealtimeNotification(
        UUID id,
        NotificationType type,
        UUID recipientUserId,
        UUID communityId,
        String title,
        String message,
        Map<String, Object> data,
        Instant timestamp
) {
    public static RealtimeNotification create(
            NotificationType type,
            UUID recipientUserId,
            UUID communityId,
            String title,
            String message,
            Map<String, Object> data
    ) {
        return new RealtimeNotification(
                UUID.randomUUID(),
                type,
                recipientUserId,
                communityId,
                title,
                message,
                data != null ? data : Map.of(),
                Instant.now()
        );
    }
}
