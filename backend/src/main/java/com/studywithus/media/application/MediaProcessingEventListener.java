package com.studywithus.media.application;

import com.studywithus.media.domain.ImageProcessorPort;
import com.studywithus.media.domain.MediaAsset;
import com.studywithus.media.domain.MediaAssetRepository;
import com.studywithus.media.domain.MediaConfirmedEvent;
import com.studywithus.media.domain.MediaStatus;
import com.studywithus.media.domain.ObjectStoragePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class MediaProcessingEventListener {

    private final MediaAssetRepository mediaAssetRepository;
    private final ObjectStoragePort objectStoragePort;
    private final ImageProcessorPort imageProcessorPort;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onMediaConfirmed(MediaConfirmedEvent event) {
        try {
            processMedia(event);
        } catch (Exception ex) {
            log.error("Async image processing failed for mediaId={}: {}", event.mediaId(), ex.getMessage(), ex);
        }
    }

    public void processMedia(MediaConfirmedEvent event) {
        MediaAsset asset = mediaAssetRepository.findById(event.mediaId()).orElse(null);
        if (asset == null || asset.getStatus() != MediaStatus.CONFIRMED) {
            return;
        }

        byte[] rawBytes = objectStoragePort.download(event.storageKey());
        ImageProcessorPort.ProcessedImage processed = imageProcessorPort.process(rawBytes, event.mimeType());

        // Overwrite original with EXIF-sanitized, compressed image
        objectStoragePort.upload(event.storageKey(), processed.sanitizedBytes(), processed.mimeType());

        // Upload thumbnail
        String thumbnailKey = buildThumbnailKey(event.storageKey());
        objectStoragePort.upload(thumbnailKey, processed.thumbnailBytes(), processed.mimeType());

        asset.markProcessed(
                thumbnailKey,
                processed.width(),
                processed.height(),
                processed.sanitizedBytes().length
        );
        mediaAssetRepository.save(asset);

        log.info("Media processed asynchronously: mediaId={}, width={}, height={}, sanitizedSize={}, thumbKey={}",
                asset.getId(), processed.width(), processed.height(), processed.sanitizedBytes().length, thumbnailKey);
    }

    private String buildThumbnailKey(String storageKey) {
        if (storageKey.startsWith("uploads/")) {
            return "thumbnails/" + storageKey.substring("uploads/".length());
        }
        return "thumbnails/" + storageKey;
    }
}
