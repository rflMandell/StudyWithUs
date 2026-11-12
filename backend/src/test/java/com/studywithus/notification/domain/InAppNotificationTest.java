package com.studywithus.notification.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InAppNotificationTest {

    @Test
    @DisplayName("Should create unread notification and mark as read")
    void shouldCreateAndMarkAsRead() {
        InAppNotification notification = InAppNotification.create(
                UUID.randomUUID(),
                NotificationType.NEW_COMMENT,
                "Novo comentário",
                "Alguém comentou",
                "CHECKIN",
                UUID.randomUUID()
        );

        assertThat(notification.isRead()).isFalse();
        assertThat(notification.getReadAt()).isNull();

        notification.markAsRead();

        assertThat(notification.isRead()).isTrue();
        assertThat(notification.getReadAt()).isNotNull();
    }
}
