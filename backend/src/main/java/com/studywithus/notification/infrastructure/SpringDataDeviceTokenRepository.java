package com.studywithus.notification.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataDeviceTokenRepository extends JpaRepository<DeviceTokenJpaEntity, UUID> {
    Optional<DeviceTokenJpaEntity> findByUserIdAndToken(UUID userId, String token);
    List<DeviceTokenJpaEntity> findByUserIdAndActiveTrue(UUID userId);
}
