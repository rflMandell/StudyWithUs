package com.studywithus.media.infrastructure;

import com.studywithus.media.domain.MediaAsset;
import com.studywithus.media.domain.MediaPurpose;
import com.studywithus.media.domain.MediaStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "media_assets")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaAssetJpaEntity {

    @Id
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MediaPurpose purpose;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MediaStatus status;

    @Column(name = "storage_key", nullable = false, unique = true, length = 500)
    private String storageKey;

    @Column(name = "thumbnail_key", length = 500)
    private String thumbnailKey;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "declared_content_type", nullable = false, length = 100)
    private String declaredContentType;

    @Column(name = "detected_mime_type", length = 100)
    private String detectedMimeType;

    @Column(name = "declared_size_bytes", nullable = false)
    private long declaredSizeBytes;

    @Column(name = "actual_size_bytes")
    private Long actualSizeBytes;

    @Column
    private Integer width;

    @Column
    private Integer height;

    @Column(name = "target_id")
    private UUID targetId;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public MediaAsset toDomain() {
        return MediaAsset.builder()
                .id(id)
                .ownerId(ownerId)
                .purpose(purpose)
                .status(status)
                .storageKey(storageKey)
                .thumbnailKey(thumbnailKey)
                .originalFilename(originalFilename)
                .declaredContentType(declaredContentType)
                .detectedMimeType(detectedMimeType)
                .declaredSizeBytes(declaredSizeBytes)
                .actualSizeBytes(actualSizeBytes)
                .width(width)
                .height(height)
                .targetId(targetId)
                .rejectionReason(rejectionReason)
                .createdAt(createdAt)
                .confirmedAt(confirmedAt)
                .processedAt(processedAt)
                .deletedAt(deletedAt)
                .build();
    }

    public static MediaAssetJpaEntity fromDomain(MediaAsset asset) {
        return MediaAssetJpaEntity.builder()
                .id(asset.getId())
                .ownerId(asset.getOwnerId())
                .purpose(asset.getPurpose())
                .status(asset.getStatus())
                .storageKey(asset.getStorageKey())
                .thumbnailKey(asset.getThumbnailKey())
                .originalFilename(asset.getOriginalFilename())
                .declaredContentType(asset.getDeclaredContentType())
                .detectedMimeType(asset.getDetectedMimeType())
                .declaredSizeBytes(asset.getDeclaredSizeBytes())
                .actualSizeBytes(asset.getActualSizeBytes())
                .width(asset.getWidth())
                .height(asset.getHeight())
                .targetId(asset.getTargetId())
                .rejectionReason(asset.getRejectionReason())
                .createdAt(asset.getCreatedAt())
                .confirmedAt(asset.getConfirmedAt())
                .processedAt(asset.getProcessedAt())
                .deletedAt(asset.getDeletedAt())
                .build();
    }
}
