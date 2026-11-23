package com.studywithus.media.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaAsset {
    private UUID id;
    private UUID ownerId;
    private MediaPurpose purpose;
    private MediaStatus status;
    private String storageKey;
    private String thumbnailKey;
    private String originalFilename;
    private String declaredContentType;
    private String detectedMimeType;
    private long declaredSizeBytes;
    private Long actualSizeBytes;
    private Integer width;
    private Integer height;
    private UUID targetId;
    private String rejectionReason;
    private Instant createdAt;
    private Instant confirmedAt;
    private Instant processedAt;
    private Instant deletedAt;

    public static MediaAsset createPending(
            UUID ownerId,
            MediaPurpose purpose,
            String storageKey,
            String originalFilename,
            String declaredContentType,
            long declaredSizeBytes,
            UUID targetId
    ) {
        return MediaAsset.builder()
                .id(UUID.randomUUID())
                .ownerId(ownerId)
                .purpose(purpose)
                .status(MediaStatus.PENDING)
                .storageKey(storageKey)
                .originalFilename(originalFilename)
                .declaredContentType(declaredContentType)
                .declaredSizeBytes(declaredSizeBytes)
                .targetId(targetId)
                .createdAt(Instant.now())
                .build();
    }

    public void confirm(String detectedMimeType, long actualSizeBytes) {
        this.detectedMimeType = detectedMimeType;
        this.actualSizeBytes = actualSizeBytes;
        this.status = MediaStatus.CONFIRMED;
        this.confirmedAt = Instant.now();
    }

    public void markProcessed(String thumbnailKey, int width, int height, long processedSizeBytes) {
        this.thumbnailKey = thumbnailKey;
        this.width = width;
        this.height = height;
        this.actualSizeBytes = processedSizeBytes;
        this.status = MediaStatus.PROCESSED;
        this.processedAt = Instant.now();
    }

    public void reject(String reason) {
        this.status = MediaStatus.REJECTED;
        this.rejectionReason = reason;
    }

    public void markDeleted() {
        this.status = MediaStatus.DELETED;
        this.deletedAt = Instant.now();
    }

    public boolean isOwnedBy(UUID userId) {
        return this.ownerId != null && this.ownerId.equals(userId);
    }

    public boolean isUsable() {
        return this.status == MediaStatus.CONFIRMED || this.status == MediaStatus.PROCESSED;
    }

    public boolean isVisible() {
        return this.status == MediaStatus.CONFIRMED || this.status == MediaStatus.PROCESSED;
    }
}
