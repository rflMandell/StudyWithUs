package com.studywithus.notification.infrastructure;

import com.studywithus.notification.domain.NotificationPreferences;
import com.studywithus.notification.domain.NotificationPreferencesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class NotificationPreferencesRepositoryAdapter implements NotificationPreferencesRepository {

    private final SpringDataNotificationPreferencesRepository repository;

    @Override
    public NotificationPreferences save(NotificationPreferences preferences) {
        if (preferences.getUpdatedAt() == null) {
            preferences.setUpdatedAt(Instant.now());
        }
        NotificationPreferencesJpaEntity entity = NotificationPreferencesJpaEntity.fromDomain(preferences);
        NotificationPreferencesJpaEntity saved = repository.save(entity);
        return saved.toDomain();
    }

    @Override
    public Optional<NotificationPreferences> findByUserId(UUID userId) {
        return repository.findById(userId).map(NotificationPreferencesJpaEntity::toDomain);
    }
}
