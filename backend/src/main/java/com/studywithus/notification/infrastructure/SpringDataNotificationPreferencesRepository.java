package com.studywithus.notification.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataNotificationPreferencesRepository extends JpaRepository<NotificationPreferencesJpaEntity, UUID> {
}
