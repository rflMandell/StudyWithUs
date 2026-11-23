package com.studywithus.media.application;

import com.studywithus.media.domain.MediaAsset;
import com.studywithus.media.domain.MediaAssetRepository;
import com.studywithus.media.domain.MediaPurpose;
import com.studywithus.media.domain.ObjectStoragePort;
import com.studywithus.shared.domain.DomainException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ValidateMediaOwnershipUseCase {

    private final MediaAssetRepository mediaAssetRepository;
    private final ObjectStoragePort objectStoragePort;

    @Transactional(readOnly = true)
    public void validateStorageKeys(List<String> storageKeys, UUID ownerId, MediaPurpose expectedPurpose) {
        if (storageKeys == null || storageKeys.isEmpty()) {
            return;
        }
        for (String key : storageKeys) {
            if (key == null || key.isBlank()) {
                throw new DomainException(HttpStatus.BAD_REQUEST, "INVALID_MEDIA_KEY", "Media storage key must not be blank");
            }
            MediaAsset asset = mediaAssetRepository.findByStorageKey(key)
                    .orElseThrow(() -> new DomainException(HttpStatus.BAD_REQUEST, "MEDIA_NOT_FOUND", "Media asset not found for key: " + key));
            validateAsset(asset, ownerId, expectedPurpose);
        }
    }

    @Transactional(readOnly = true)
    public MediaAssetResult validateAndResolveByMediaId(UUID mediaId, UUID ownerId, MediaPurpose expectedPurpose) {
        MediaAsset asset = mediaAssetRepository.findById(mediaId)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "MEDIA_NOT_FOUND", "Media asset not found"));
        validateAsset(asset, ownerId, expectedPurpose);
        String url = objectStoragePort.getPublicUrl(asset.getStorageKey());
        String thumbUrl = asset.getThumbnailKey() != null ? objectStoragePort.getPublicUrl(asset.getThumbnailKey()) : null;
        return MediaAssetResult.from(asset, url, thumbUrl);
    }

    private void validateAsset(MediaAsset asset, UUID ownerId, MediaPurpose expectedPurpose) {
        if (!asset.isOwnedBy(ownerId)) {
            throw new DomainException(HttpStatus.FORBIDDEN, "MEDIA_OWNERSHIP_DENIED", "You do not own the referenced media asset");
        }
        if (asset.getPurpose() != expectedPurpose) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "MEDIA_PURPOSE_MISMATCH",
                    "Media asset purpose is " + asset.getPurpose() + ", expected " + expectedPurpose);
        }
        if (!asset.isUsable()) {
            throw new DomainException(HttpStatus.UNPROCESSABLE_ENTITY, "MEDIA_NOT_CONFIRMED",
                    "Media asset must be confirmed before association (current status: " + asset.getStatus() + ")");
        }
    }
}
