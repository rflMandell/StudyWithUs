package com.studywithus.identity.presentation;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record UpdateAvatarRequest(
        @NotNull(message = "mediaId is required")
        UUID mediaId
) {
}
