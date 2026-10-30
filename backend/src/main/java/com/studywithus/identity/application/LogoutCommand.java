package com.studywithus.identity.application;

import java.util.UUID;

public record LogoutCommand(
        String refreshToken,
        UUID userId
) {
}
