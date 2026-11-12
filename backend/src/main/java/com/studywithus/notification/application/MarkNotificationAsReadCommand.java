package com.studywithus.notification.application;

import java.util.UUID;

public record MarkNotificationAsReadCommand(
        UUID notificationId,
        UUID userId
) {
}
