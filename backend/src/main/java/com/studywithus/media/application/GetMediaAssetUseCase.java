package com.studywithus.media.application;

import com.studywithus.media.domain.MediaAsset;
import com.studywithus.media.domain.MediaAssetRepository;
import com.studywithus.media.domain.ObjectStoragePort;
import com.studywithus.shared.domain.DomainException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetMediaAssetUseCase {

    private final MediaAssetRepository mediaAssetRepository;
    private final ObjectStoragePort objectStoragePort;

    @Transactional(readOnly = true)
    public MediaAssetResult execute(UUID mediaId) {
        MediaAsset asset = mediaAssetRepository.findById(mediaId)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "MEDIA_NOT_FOUND", "Media asset not found"));

        String url = asset.isVisible() ? objectStoragePort.getPublicUrl(asset.getStorageKey()) : null;
        String thumbUrl = (asset.isVisible() && asset.getThumbnailKey() != null)
                ? objectStoragePort.getPublicUrl(asset.getThumbnailKey())
                : null;
        return MediaAssetResult.from(asset, url, thumbUrl);
    }
}
