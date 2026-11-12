package com.studywithus.notification.application;

import com.studywithus.notification.domain.InAppNotification;
import com.studywithus.notification.domain.NotificationType;

import java.time.Instant;
import java.util.UUID;

public record NotificationResult(
        UUID id,
        UUID userId,
        NotificationType type,
        String title,
        String body,
        String referenceType,
        UUID referenceId,
        boolean read,
        Instant readAt,
        Instant createdAt
) {
    public static NotificationResult fromDomain(InAppNotification notification) {
        return new NotificationResult(
                notification.getId(),
                notification.getUserId(),
                notification.getType(),
                notification.getTitle(),
                notification.getBody(),
                notification.getReferenceType(),
                notification.getReferenceId(),
                notification.isRead(),
                notification.getReadAt(),
                notification.getCreatedAt()
        );
    }
}
