package com.studywithus.media.domain;

import java.util.Optional;
import java.util.UUID;

public interface MediaAssetRepository {
    MediaAsset save(MediaAsset mediaAsset);
    Optional<MediaAsset> findById(UUID id);
    Optional<MediaAsset> findByStorageKey(String storageKey);
}
