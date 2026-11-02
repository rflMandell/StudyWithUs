package com.studywithus.checkin.presentation;

import com.studywithus.checkin.application.CreateCheckinCommand;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record CreateCheckinRequest(
        @NotNull(message = "Community ID is required")
        UUID communityId,

        @Size(max = 1000, message = "Caption must not exceed 1000 characters")
        String caption,

        List<String> mediaKeys
) {
    public CreateCheckinCommand toCommand(UUID userId, String idempotencyKey) {
        return new CreateCheckinCommand(
                userId,
                communityId,
                caption,
                mediaKeys,
                idempotencyKey
        );
    }
}
