package com.studywithus.media.presentation;

import com.studywithus.media.application.MediaAssetResult;
import com.studywithus.media.domain.MediaPurpose;
import com.studywithus.media.domain.MediaStatus;

import java.time.Instant;
import java.util.UUID;

public record MediaAssetResponse(
        UUID id,
        UUID ownerId,
        MediaPurpose purpose,
        MediaStatus status,
        String storageKey,
        String thumbnailKey,
        String url,
        String thumbnailUrl,
        String detectedMimeType,
        Long sizeBytes,
        Integer width,
        Integer height,
        Instant createdAt,
        Instant confirmedAt,
        Instant processedAt
) {
    public static MediaAssetResponse from(MediaAssetResult result) {
        return new MediaAssetResponse(
                result.id(),
                result.ownerId(),
                result.purpose(),
                result.status(),
                result.storageKey(),
                result.thumbnailKey(),
                result.url(),
                result.thumbnailUrl(),
                result.detectedMimeType(),
                result.sizeBytes(),
                result.width(),
                result.height(),
                result.createdAt(),
                result.confirmedAt(),
                result.processedAt()
        );
    }
}
