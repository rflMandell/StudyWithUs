package com.studywithus.notification.application;

import com.studywithus.notification.domain.NotificationRepository;
import com.studywithus.shared.application.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetUnreadNotificationsCountUseCase implements UseCase<UUID, Long> {

    private final NotificationRepository notificationRepository;

    @Override
    @Transactional(readOnly = true)
    public Long execute(UUID userId) {
        return notificationRepository.countUnreadByUserId(userId);
    }
}
