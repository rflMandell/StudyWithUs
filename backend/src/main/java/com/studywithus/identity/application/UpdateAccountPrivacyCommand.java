package com.studywithus.identity.application;

import java.util.UUID;

public record UpdateAccountPrivacyCommand(
        UUID userId,
        boolean isPrivate
) {
}
