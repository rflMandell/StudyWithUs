package com.studywithus.notification.presentation;

import com.studywithus.notification.application.NotificationPreferencesResult;

import java.time.Instant;
import java.util.UUID;

public record NotificationPreferencesResponse(
        UUID userId,
        boolean commentsEnabled,
        boolean rankingEnabled,
        boolean challengeEnabled,
        boolean invitationEnabled,
        boolean reminderEnabled,
        boolean streakEnabled,
        Instant updatedAt
) {
    public static NotificationPreferencesResponse from(NotificationPreferencesResult result) {
        return new NotificationPreferencesResponse(
                result.userId(),
                result.commentsEnabled(),
                result.rankingEnabled(),
                result.challengeEnabled(),
                result.invitationEnabled(),
                result.reminderEnabled(),
                result.streakEnabled(),
                result.updatedAt()
        );
    }
}
