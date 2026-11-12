package com.studywithus.notification.application;

import java.util.UUID;

public record RegisterDeviceTokenCommand(
        UUID userId,
        String token,
        String platform
) {
}
