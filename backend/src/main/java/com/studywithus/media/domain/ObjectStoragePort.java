package com.studywithus.media.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

public interface ObjectStoragePort {

    PresignedUploadResult generatePresignedUploadUrl(
            String storageKey,
            String contentType,
            long contentLength,
            Duration expiration
    );

    boolean exists(String storageKey);

    StoredObjectMetadata getMetadata(String storageKey);

    byte[] download(String storageKey);

    void upload(String storageKey, byte[] data, String contentType);

    void delete(String storageKey);

    String getPublicUrl(String storageKey);

    default StorageHealth checkHealth() {
        return new StorageHealth(true, "LOCAL", "local", 0L, null);
    }

    record StorageHealth(
            boolean up,
            String provider,
            String bucketOrPath,
            long latencyMs,
            String error
    ) {}

    record PresignedUploadResult(
            String presignedUrl,
            Instant expiresAt,
            Map<String, String> requiredHeaders
    ) {}

    record StoredObjectMetadata(
            long contentLength,
            String contentType
    ) {}
}
