package com.studywithus.notification.presentation;

import com.studywithus.notification.application.NotificationResult;
import com.studywithus.notification.domain.NotificationType;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        NotificationType type,
        String title,
        String body,
        String referenceType,
        UUID referenceId,
        boolean read,
        Instant readAt,
        Instant createdAt
) {
    public static NotificationResponse from(NotificationResult result) {
        return new NotificationResponse(
                result.id(),
                result.type(),
                result.title(),
                result.body(),
                result.referenceType(),
                result.referenceId(),
                result.read(),
                result.readAt(),
                result.createdAt()
        );
    }
}
