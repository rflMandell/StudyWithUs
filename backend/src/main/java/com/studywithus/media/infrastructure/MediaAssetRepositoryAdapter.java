package com.studywithus.media.infrastructure;

import com.studywithus.media.domain.MediaAsset;
import com.studywithus.media.domain.MediaAssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class MediaAssetRepositoryAdapter implements MediaAssetRepository {

    private final SpringDataMediaAssetRepository jpaRepository;

    @Override
    public MediaAsset save(MediaAsset mediaAsset) {
        MediaAssetJpaEntity entity = MediaAssetJpaEntity.fromDomain(mediaAsset);
        return jpaRepository.save(entity).toDomain();
    }

    @Override
    public Optional<MediaAsset> findById(UUID id) {
        return jpaRepository.findById(id).map(MediaAssetJpaEntity::toDomain);
    }

    @Override
    public Optional<MediaAsset> findByStorageKey(String storageKey) {
        return jpaRepository.findByStorageKey(storageKey).map(MediaAssetJpaEntity::toDomain);
    }
}
