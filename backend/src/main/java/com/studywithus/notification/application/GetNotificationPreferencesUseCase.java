package com.studywithus.notification.application;

import com.studywithus.notification.domain.NotificationPreferences;
import com.studywithus.notification.domain.NotificationPreferencesRepository;
import com.studywithus.shared.application.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetNotificationPreferencesUseCase implements UseCase<UUID, NotificationPreferencesResult> {

    private final NotificationPreferencesRepository repository;

    @Override
    @Transactional(readOnly = true)
    public NotificationPreferencesResult execute(UUID userId) {
        NotificationPreferences prefs = repository.findByUserId(userId)
                .orElseGet(() -> NotificationPreferences.createDefault(userId));
        return NotificationPreferencesResult.fromDomain(prefs);
    }
}
