package com.studywithus.notification.application;

import com.studywithus.notification.domain.NotificationType;

import java.util.UUID;

public record CreateNotificationCommand(
        UUID userId,
        NotificationType type,
        String title,
        String body,
        String referenceType,
        UUID referenceId
) {
}
