package com.studywithus.notification.domain;

import com.studywithus.shared.domain.PagedResult;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository {
    InAppNotification save(InAppNotification notification);
    Optional<InAppNotification> findById(UUID id);
    PagedResult<InAppNotification> findByUserId(UUID userId, boolean unreadOnly, int page, int size);
    long countUnreadByUserId(UUID userId);
    void markAllAsReadByUserId(UUID userId, Instant readAt);
}
