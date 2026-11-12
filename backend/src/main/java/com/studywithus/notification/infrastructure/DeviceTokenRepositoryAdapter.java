package com.studywithus.notification.infrastructure;

import com.studywithus.notification.domain.DeviceToken;
import com.studywithus.notification.domain.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DeviceTokenRepositoryAdapter implements DeviceTokenRepository {

    private final SpringDataDeviceTokenRepository repository;

    @Override
    public DeviceToken save(DeviceToken deviceToken) {
        if (deviceToken.getId() == null) {
            deviceToken.setId(UUID.randomUUID());
        }
        if (deviceToken.getCreatedAt() == null) {
            deviceToken.setCreatedAt(Instant.now());
        }
        if (deviceToken.getLastSeenAt() == null) {
            deviceToken.setLastSeenAt(Instant.now());
        }
        DeviceTokenJpaEntity entity = DeviceTokenJpaEntity.fromDomain(deviceToken);
        DeviceTokenJpaEntity saved = repository.save(entity);
        return saved.toDomain();
    }

    @Override
    public Optional<DeviceToken> findByUserIdAndToken(UUID userId, String token) {
        return repository.findByUserIdAndToken(userId, token).map(DeviceTokenJpaEntity::toDomain);
    }

    @Override
    public List<DeviceToken> findActiveTokensByUserId(UUID userId) {
        return repository.findByUserIdAndActiveTrue(userId)
                .stream()
                .map(DeviceTokenJpaEntity::toDomain)
                .toList();
    }
}
