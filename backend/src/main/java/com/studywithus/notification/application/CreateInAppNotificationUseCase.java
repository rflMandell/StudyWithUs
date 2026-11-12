package com.studywithus.notification.application;

import com.studywithus.notification.domain.InAppNotification;
import com.studywithus.notification.domain.NotificationPreferences;
import com.studywithus.notification.domain.NotificationPreferencesRepository;
import com.studywithus.notification.domain.NotificationRepository;
import com.studywithus.notification.domain.PushNotificationPort;
import com.studywithus.notification.domain.RealtimeNotification;
import com.studywithus.notification.domain.RealtimeNotificationPort;
import com.studywithus.shared.application.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreateInAppNotificationUseCase implements UseCase<CreateNotificationCommand, Optional<NotificationResult>> {

    private final NotificationRepository notificationRepository;
    private final NotificationPreferencesRepository preferencesRepository;
    private final PushNotificationPort pushNotificationPort;
    private final RealtimeNotificationPort realtimeNotificationPort;

    @Override
    @Transactional
    public Optional<NotificationResult> execute(CreateNotificationCommand command) {
        NotificationPreferences preferences = preferencesRepository.findByUserId(command.userId())
                .orElseGet(() -> NotificationPreferences.createDefault(command.userId()));

        if (!preferences.isCategoryEnabled(command.type())) {
            log.debug("Notification of type {} suppressed by user {} preferences", command.type(), command.userId());
            return Optional.empty();
        }

        InAppNotification notification = InAppNotification.create(
                command.userId(),
                command.type(),
                command.title(),
                command.body(),
                command.referenceType(),
                command.referenceId()
        );

        InAppNotification saved = notificationRepository.save(notification);
        log.info("Persisted in-app notification {} for user {} (type={})", saved.getId(), saved.getUserId(), saved.getType());

        // Dispatch push notification safely
        Map<String, String> pushData = new HashMap<>();
        pushData.put("notificationId", saved.getId().toString());
        pushData.put("type", saved.getType().name());
        if (saved.getReferenceType() != null) {
            pushData.put("referenceType", saved.getReferenceType());
        }
        if (saved.getReferenceId() != null) {
            pushData.put("referenceId", saved.getReferenceId().toString());
        }
        pushNotificationPort.sendPushNotification(saved.getUserId(), saved.getTitle(), saved.getBody(), pushData);

        // Also push via SSE realtime if user is connected
        try {
            Map<String, Object> realtimeData = new HashMap<>(pushData);
            realtimeNotificationPort.sendToUser(saved.getUserId(), RealtimeNotification.create(
                    saved.getType(),
                    saved.getUserId(),
                    null,
                    saved.getTitle(),
                    saved.getBody(),
                    realtimeData
            ));
        } catch (Exception ex) {
            log.warn("Failed to dispatch SSE notification: {}", ex.getMessage());
        }

        return Optional.of(NotificationResult.fromDomain(saved));
    }
}
