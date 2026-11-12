package com.studywithus.notification.application;

import com.studywithus.notification.domain.InAppNotification;
import com.studywithus.notification.domain.NotificationPreferences;
import com.studywithus.notification.domain.NotificationPreferencesRepository;
import com.studywithus.notification.domain.NotificationRepository;
import com.studywithus.notification.domain.NotificationType;
import com.studywithus.notification.domain.PushNotificationPort;
import com.studywithus.notification.domain.RealtimeNotificationPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateInAppNotificationUseCaseTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationPreferencesRepository preferencesRepository;

    @Mock
    private PushNotificationPort pushNotificationPort;

    @Mock
    private RealtimeNotificationPort realtimeNotificationPort;

    @InjectMocks
    private CreateInAppNotificationUseCase useCase;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Creates and persists notification when category is enabled")
    void shouldCreateNotificationWhenEnabled() {
        NotificationPreferences prefs = NotificationPreferences.createDefault(userId);
        when(preferencesRepository.findByUserId(userId)).thenReturn(Optional.of(prefs));
        when(notificationRepository.save(any(InAppNotification.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateNotificationCommand command = new CreateNotificationCommand(
                userId,
                NotificationType.NEW_COMMENT,
                "Novo comentário",
                "Comentaram no seu check-in",
                "CHECKIN",
                UUID.randomUUID()
        );

        Optional<NotificationResult> result = useCase.execute(command);

        assertThat(result).isPresent();
        assertThat(result.get().title()).isEqualTo("Novo comentário");
        verify(notificationRepository).save(any(InAppNotification.class));
        verify(pushNotificationPort).sendPushNotification(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Suppresses notification when category is disabled by preferences")
    void shouldSuppressNotificationWhenDisabled() {
        NotificationPreferences prefs = NotificationPreferences.createDefault(userId);
        prefs.setCommentsEnabled(false);
        when(preferencesRepository.findByUserId(userId)).thenReturn(Optional.of(prefs));

        CreateNotificationCommand command = new CreateNotificationCommand(
                userId,
                NotificationType.NEW_COMMENT,
                "Novo comentário",
                "Comentaram no seu check-in",
                "CHECKIN",
                UUID.randomUUID()
        );

        Optional<NotificationResult> result = useCase.execute(command);

        assertThat(result).isEmpty();
        verify(notificationRepository, never()).save(any());
        verify(pushNotificationPort, never()).sendPushNotification(any(), any(), any(), any());
    }
}
