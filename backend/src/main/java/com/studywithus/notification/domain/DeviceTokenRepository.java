package com.studywithus.notification.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceTokenRepository {
    DeviceToken save(DeviceToken deviceToken);
    Optional<DeviceToken> findByUserIdAndToken(UUID userId, String token);
    List<DeviceToken> findActiveTokensByUserId(UUID userId);
}
