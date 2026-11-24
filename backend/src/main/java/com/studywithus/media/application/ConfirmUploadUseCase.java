package com.studywithus.media.application;

import com.studywithus.media.domain.ImageProcessorPort;
import com.studywithus.media.domain.MediaAsset;
import com.studywithus.media.domain.MediaAssetRepository;
import com.studywithus.media.domain.MediaConfirmedEvent;
import com.studywithus.media.domain.MediaStatus;
import com.studywithus.media.domain.ObjectStoragePort;
import com.studywithus.shared.application.UseCase;
import com.studywithus.shared.domain.DomainException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConfirmUploadUseCase implements UseCase<ConfirmUploadCommand, MediaAssetResult> {

    private final MediaAssetRepository mediaAssetRepository;
    private final ObjectStoragePort objectStoragePort;
    private final ImageProcessorPort imageProcessorPort;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${app.media.max-file-size-bytes:5242880}")
    private long maxFileSizeBytes = 5242880L;

    @Override
    @Transactional
    public MediaAssetResult execute(ConfirmUploadCommand command) {
        MediaAsset asset = mediaAssetRepository.findById(command.mediaId())
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "MEDIA_NOT_FOUND", "Media asset not found"));

        if (!asset.isOwnedBy(command.userId())) {
            throw new DomainException(HttpStatus.FORBIDDEN, "FORBIDDEN", "You do not own this media asset");
        }

        if (asset.getStatus() == MediaStatus.CONFIRMED || asset.getStatus() == MediaStatus.PROCESSED) {
            return toResult(asset);
        }

        if (asset.getStatus() != MediaStatus.PENDING) {
            throw new DomainException(HttpStatus.CONFLICT, "MEDIA_INVALID_STATUS", "Media asset cannot be confirmed in status " + asset.getStatus());
        }

        if (!objectStoragePort.exists(asset.getStorageKey())) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "MEDIA_NOT_UPLOADED", "Uploaded file not found in object storage");
        }

        ObjectStoragePort.StoredObjectMetadata metadata = objectStoragePort.getMetadata(asset.getStorageKey());
        if (metadata.contentLength() <= 0 || metadata.contentLength() > maxFileSizeBytes) {
            rejectAndCleanup(asset, "File size exceeds allowed limits: " + metadata.contentLength());
            throw new DomainException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_MEDIA_SIZE", "Uploaded file size is invalid or exceeds limit");
        }

        byte[] rawBytes = objectStoragePort.download(asset.getStorageKey());
        String detectedMimeType = MediaValidationService.detectRealMimeType(rawBytes)
                .orElse(null);

        if (detectedMimeType == null) {
            rejectAndCleanup(asset, "Unrecognized or unsupported image magic bytes");
            throw new DomainException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_MEDIA_MIME_TYPE", "File is not a valid JPEG, PNG, or WebP image");
        }

        if (!MediaValidationService.matchesExtension(detectedMimeType, asset.getOriginalFilename())) {
            rejectAndCleanup(asset, "Detected MIME type " + detectedMimeType + " does not match filename extension");
            throw new DomainException(HttpStatus.UNPROCESSABLE_ENTITY, "MEDIA_EXTENSION_MISMATCH", "File content does not match declared file extension");
        }

        ImageProcessorPort.ProcessedImage processed = imageProcessorPort.process(rawBytes, detectedMimeType);

        String originalStorageKey = asset.getStorageKey();
        String webpStorageKey = toWebpKey(originalStorageKey);
        String thumbnailKey = buildThumbnailKey(webpStorageKey);

        objectStoragePort.upload(webpStorageKey, processed.sanitizedBytes(), processed.mimeType());
        objectStoragePort.upload(thumbnailKey, processed.thumbnailBytes(), processed.mimeType());

        if (!webpStorageKey.equals(originalStorageKey)) {
            try {
                objectStoragePort.delete(originalStorageKey);
            } catch (Exception ex) {
                log.warn("Failed to delete pre-conversion object {}: {}", originalStorageKey, ex.getMessage());
            }
        }

        asset.setStorageKey(webpStorageKey);
        asset.confirm(processed.mimeType(), processed.sanitizedBytes().length);
        asset.markProcessed(
                thumbnailKey,
                processed.width(),
                processed.height(),
                processed.sanitizedBytes().length
        );
        MediaAsset saved = mediaAssetRepository.save(asset);

        log.info("Media upload confirmed and converted to WebP: mediaId={}, ownerId={}, originalMime={}, finalKey={}, size={}",
                saved.getId(), saved.getOwnerId(), detectedMimeType, webpStorageKey, processed.sanitizedBytes().length);

        eventPublisher.publishEvent(new MediaConfirmedEvent(
                saved.getId(),
                saved.getOwnerId(),
                saved.getStorageKey(),
                processed.mimeType()
        ));

        return toResult(saved);
    }

    static String toWebpKey(String storageKey) {
        int lastDot = storageKey.lastIndexOf('.');
        int lastSlash = storageKey.lastIndexOf('/');
        if (lastDot > lastSlash) {
            return storageKey.substring(0, lastDot) + ".webp";
        }
        return storageKey + ".webp";
    }

    static String buildThumbnailKey(String storageKey) {
        if (storageKey.startsWith("uploads/")) {
            return "thumbnails/" + storageKey.substring("uploads/".length());
        }
        return "thumbnails/" + storageKey;
    }

    private void rejectAndCleanup(MediaAsset asset, String reason) {
        asset.reject(reason);
        mediaAssetRepository.save(asset);
        try {
            objectStoragePort.delete(asset.getStorageKey());
        } catch (Exception ex) {
            log.warn("Failed to cleanup rejected object {}: {}", asset.getStorageKey(), ex.getMessage());
        }
    }

    private MediaAssetResult toResult(MediaAsset asset) {
        String url = objectStoragePort.getPublicUrl(asset.getStorageKey());
        String thumbUrl = asset.getThumbnailKey() != null
                ? objectStoragePort.getPublicUrl(asset.getThumbnailKey())
                : null;
        return MediaAssetResult.from(asset, url, thumbUrl);
    }
}
