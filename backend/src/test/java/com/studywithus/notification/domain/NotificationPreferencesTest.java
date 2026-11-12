package com.studywithus.notification.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationPreferencesTest {

    @Test
    @DisplayName("Should evaluate category enablement based on preference flags")
    void shouldEvaluateCategoryEnablement() {
        NotificationPreferences prefs = NotificationPreferences.createDefault(UUID.randomUUID());

        assertThat(prefs.isCategoryEnabled(NotificationType.NEW_COMMENT)).isTrue();
        assertThat(prefs.isCategoryEnabled(NotificationType.CHALLENGE_COMPLETED)).isTrue();

        prefs.setCommentsEnabled(false);
        assertThat(prefs.isCategoryEnabled(NotificationType.NEW_COMMENT)).isFalse();
        assertThat(prefs.isCategoryEnabled(NotificationType.CHALLENGE_COMPLETED)).isTrue();

        prefs.setChallengeEnabled(false);
        assertThat(prefs.isCategoryEnabled(NotificationType.CHALLENGE_COMPLETED)).isFalse();
    }
}
