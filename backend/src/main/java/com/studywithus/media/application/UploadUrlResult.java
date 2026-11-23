package com.studywithus.media.application;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record UploadUrlResult(
        UUID mediaId,
        String storageKey,
        String presignedUrl,
        Instant expiresAt,
        Map<String, String> requiredHeaders
) {
}
