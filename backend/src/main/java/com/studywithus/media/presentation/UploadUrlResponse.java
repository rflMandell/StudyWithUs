package com.studywithus.media.presentation;

import com.studywithus.media.application.UploadUrlResult;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record UploadUrlResponse(
        UUID mediaId,
        String storageKey,
        String presignedUrl,
        Instant expiresAt,
        Map<String, String> requiredHeaders
) {
    public static UploadUrlResponse from(UploadUrlResult result) {
        return new UploadUrlResponse(
                result.mediaId(),
                result.storageKey(),
                result.presignedUrl(),
                result.expiresAt(),
                result.requiredHeaders()
        );
    }
}
