package com.studywithus.notification.infrastructure;

import com.studywithus.notification.domain.InAppNotification;
import com.studywithus.notification.domain.NotificationRepository;
import com.studywithus.shared.domain.PagedResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class NotificationRepositoryAdapter implements NotificationRepository {

    private final SpringDataNotificationRepository repository;

    @Override
    public InAppNotification save(InAppNotification notification) {
        if (notification.getId() == null) {
            notification.setId(UUID.randomUUID());
        }
        if (notification.getCreatedAt() == null) {
            notification.setCreatedAt(Instant.now());
        }
        NotificationJpaEntity entity = NotificationJpaEntity.fromDomain(notification);
        NotificationJpaEntity saved = repository.save(entity);
        return saved.toDomain();
    }

    @Override
    public Optional<InAppNotification> findById(UUID id) {
        return repository.findById(id).map(NotificationJpaEntity::toDomain);
    }

    @Override
    public PagedResult<InAppNotification> findByUserId(UUID userId, boolean unreadOnly, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<NotificationJpaEntity> resultPage = unreadOnly
                ? repository.findByUserIdAndReadAtIsNullOrderByCreatedAtDesc(userId, pageRequest)
                : repository.findByUserIdOrderByCreatedAtDesc(userId, pageRequest);

        return PagedResult.of(
                resultPage.getContent().stream().map(NotificationJpaEntity::toDomain).toList(),
                resultPage.getNumber(),
                resultPage.getSize(),
                resultPage.getTotalElements()
        );
    }

    @Override
    public long countUnreadByUserId(UUID userId) {
        return repository.countByUserIdAndReadAtIsNull(userId);
    }

    @Override
    public void markAllAsReadByUserId(UUID userId, Instant readAt) {
        repository.markAllAsRead(userId, readAt != null ? readAt : Instant.now());
    }
}
