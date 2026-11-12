package com.studywithus.notification.domain;

import java.util.Optional;
import java.util.UUID;

public interface NotificationPreferencesRepository {
    NotificationPreferences save(NotificationPreferences preferences);
    Optional<NotificationPreferences> findByUserId(UUID userId);
}
