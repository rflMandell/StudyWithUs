package com.studywithus.notification.presentation;

import com.studywithus.notification.application.UpdateNotificationPreferencesCommand;

import java.util.UUID;

public record UpdateNotificationPreferencesRequest(
        Boolean commentsEnabled,
        Boolean rankingEnabled,
        Boolean challengeEnabled,
        Boolean invitationEnabled,
        Boolean reminderEnabled,
        Boolean streakEnabled
) {
    public UpdateNotificationPreferencesCommand toCommand(UUID userId) {
        return new UpdateNotificationPreferencesCommand(
                userId,
                commentsEnabled,
                rankingEnabled,
                challengeEnabled,
                invitationEnabled,
                reminderEnabled,
                streakEnabled
        );
    }
}
