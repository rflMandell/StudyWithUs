package com.studywithus.identity.application;

import java.util.UUID;

public record ChangePasswordCommand(
        UUID userId,
        String currentPassword,
        String newPassword,
        String confirmPassword
) {
}
