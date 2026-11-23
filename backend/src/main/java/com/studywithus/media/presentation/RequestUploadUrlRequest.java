package com.studywithus.media.presentation;

import com.studywithus.media.application.RequestUploadUrlCommand;
import com.studywithus.media.domain.MediaPurpose;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record RequestUploadUrlRequest(
        @NotNull(message = "Purpose is required")
        MediaPurpose purpose,

        @NotBlank(message = "Filename is required")
        String filename,

        @NotBlank(message = "Content-Type is required")
        String contentType,

        @Positive(message = "File size must be positive")
        long sizeBytes,

        UUID targetId
) {
    public RequestUploadUrlCommand toCommand(UUID userId) {
        return new RequestUploadUrlCommand(userId, purpose, filename, contentType, sizeBytes, targetId);
    }
}
