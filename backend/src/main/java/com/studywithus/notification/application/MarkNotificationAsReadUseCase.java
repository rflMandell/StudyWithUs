package com.studywithus.notification.application;

import com.studywithus.notification.domain.InAppNotification;
import com.studywithus.notification.domain.NotificationRepository;
import com.studywithus.shared.application.UseCase;
import com.studywithus.shared.domain.DomainException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MarkNotificationAsReadUseCase implements UseCase<MarkNotificationAsReadCommand, NotificationResult> {

    private final NotificationRepository notificationRepository;

    @Override
    @Transactional
    public NotificationResult execute(MarkNotificationAsReadCommand command) {
        InAppNotification notification = notificationRepository.findById(command.notificationId())
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "NOTIFICATION_NOT_FOUND", "Notificação não encontrada"));

        if (!notification.getUserId().equals(command.userId())) {
            throw new DomainException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Você não tem permissão para alterar esta notificação");
        }

        notification.markAsRead();
        InAppNotification saved = notificationRepository.save(notification);

        return NotificationResult.fromDomain(saved);
    }
}
