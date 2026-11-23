package com.studywithus.media.application;

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

        asset.confirm(detectedMimeType, rawBytes.length);
        MediaAsset saved = mediaAssetRepository.save(asset);

        log.info("Media upload confirmed: mediaId={}, ownerId={}, mimeType={}, size={}",
                saved.getId(), saved.getOwnerId(), detectedMimeType, rawBytes.length);

        eventPublisher.publishEvent(new MediaConfirmedEvent(
                saved.getId(),
                saved.getOwnerId(),
                saved.getStorageKey(),
                detectedMimeType
        ));

        return toResult(saved);
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
