package com.studywithus.notification.application;

import com.studywithus.notification.domain.NotificationPreferences;

import java.time.Instant;
import java.util.UUID;

public record NotificationPreferencesResult(
        UUID userId,
        boolean commentsEnabled,
        boolean rankingEnabled,
        boolean challengeEnabled,
        boolean invitationEnabled,
        boolean reminderEnabled,
        boolean streakEnabled,
        Instant updatedAt
) {
    public static NotificationPreferencesResult fromDomain(NotificationPreferences domain) {
        return new NotificationPreferencesResult(
                domain.getUserId(),
                domain.isCommentsEnabled(),
                domain.isRankingEnabled(),
                domain.isChallengeEnabled(),
                domain.isInvitationEnabled(),
                domain.isReminderEnabled(),
                domain.isStreakEnabled(),
                domain.getUpdatedAt()
        );
    }
}
