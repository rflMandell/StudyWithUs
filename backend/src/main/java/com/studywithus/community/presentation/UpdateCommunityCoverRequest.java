package com.studywithus.community.presentation;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record UpdateCommunityCoverRequest(
        @NotNull(message = "mediaId is required")
        UUID mediaId
) {
}
