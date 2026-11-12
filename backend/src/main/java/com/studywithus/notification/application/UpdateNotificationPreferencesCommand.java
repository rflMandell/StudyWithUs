package com.studywithus.notification.application;

import java.util.UUID;

public record UpdateNotificationPreferencesCommand(
        UUID userId,
        Boolean commentsEnabled,
        Boolean rankingEnabled,
        Boolean challengeEnabled,
        Boolean invitationEnabled,
        Boolean reminderEnabled,
        Boolean streakEnabled
) {
}
