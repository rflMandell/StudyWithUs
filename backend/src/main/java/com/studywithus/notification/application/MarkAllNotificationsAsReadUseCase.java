package com.studywithus.notification.application;

import com.studywithus.notification.domain.NotificationRepository;
import com.studywithus.shared.application.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MarkAllNotificationsAsReadUseCase implements UseCase<UUID, Void> {

    private final NotificationRepository notificationRepository;

    @Override
    @Transactional
    public Void execute(UUID userId) {
        notificationRepository.markAllAsReadByUserId(userId, Instant.now());
        return null;
    }
}
