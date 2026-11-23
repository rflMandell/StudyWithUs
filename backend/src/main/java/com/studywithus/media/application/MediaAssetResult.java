package com.studywithus.media.application;

import com.studywithus.media.domain.MediaAsset;
import com.studywithus.media.domain.MediaPurpose;
import com.studywithus.media.domain.MediaStatus;

import java.time.Instant;
import java.util.UUID;

public record MediaAssetResult(
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
    public static MediaAssetResult from(MediaAsset asset, String url, String thumbnailUrl) {
        return new MediaAssetResult(
                asset.getId(),
                asset.getOwnerId(),
                asset.getPurpose(),
                asset.getStatus(),
                asset.getStorageKey(),
                asset.getThumbnailKey(),
                url,
                thumbnailUrl,
                asset.getDetectedMimeType(),
                asset.getActualSizeBytes() != null ? asset.getActualSizeBytes() : asset.getDeclaredSizeBytes(),
                asset.getWidth(),
                asset.getHeight(),
                asset.getCreatedAt(),
                asset.getConfirmedAt(),
                asset.getProcessedAt()
        );
    }
}
