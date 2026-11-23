package com.studywithus.media.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataMediaAssetRepository extends JpaRepository<MediaAssetJpaEntity, UUID> {
    Optional<MediaAssetJpaEntity> findByStorageKey(String storageKey);
}
