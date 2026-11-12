package com.studywithus.notification.application;

import com.studywithus.notification.domain.InAppNotification;
import com.studywithus.notification.domain.NotificationRepository;
import com.studywithus.notification.domain.NotificationType;
import com.studywithus.shared.domain.DomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarkNotificationAsReadUseCaseTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private MarkNotificationAsReadUseCase useCase;

    private UUID userId;
    private UUID notificationId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        notificationId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Marks notification as read when requested by recipient")
    void shouldMarkAsReadSuccessfully() {
        InAppNotification notification = InAppNotification.create(
                userId,
                NotificationType.NEW_COMMENT,
                "Título",
                "Corpo",
                "CHECKIN",
                UUID.randomUUID()
        );
        notification.setId(notificationId);

        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(InAppNotification.class))).thenAnswer(inv -> inv.getArgument(0));

        NotificationResult result = useCase.execute(new MarkNotificationAsReadCommand(notificationId, userId));

        assertThat(result.read()).isTrue();
        assertThat(result.readAt()).isNotNull();
        verify(notificationRepository).save(notification);
    }

    @Test
    @DisplayName("Throws FORBIDDEN when user is not the recipient of the notification")
    void shouldThrowForbiddenWhenUserIsNotRecipient() {
        InAppNotification notification = InAppNotification.create(
                UUID.randomUUID(), // different user
                NotificationType.NEW_COMMENT,
                "Título",
                "Corpo",
                "CHECKIN",
                UUID.randomUUID()
        );
        notification.setId(notificationId);

        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));

        assertThatThrownBy(() -> useCase.execute(new MarkNotificationAsReadCommand(notificationId, userId)))
                .isInstanceOf(DomainException.class)
                .matches(e -> ((DomainException) e).getStatus() == HttpStatus.FORBIDDEN);
    }
}
