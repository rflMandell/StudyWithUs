package com.studywithus.notification.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreferences {
    private UUID userId;
    @Builder.Default
    private boolean commentsEnabled = true;
    @Builder.Default
    private boolean rankingEnabled = true;
    @Builder.Default
    private boolean challengeEnabled = true;
    @Builder.Default
    private boolean invitationEnabled = true;
    @Builder.Default
    private boolean reminderEnabled = true;
    @Builder.Default
    private boolean streakEnabled = true;
    private Instant updatedAt;

    public static NotificationPreferences createDefault(UUID userId) {
        return NotificationPreferences.builder()
                .userId(userId)
                .commentsEnabled(true)
                .rankingEnabled(true)
                .challengeEnabled(true)
                .invitationEnabled(true)
                .reminderEnabled(true)
                .streakEnabled(true)
                .updatedAt(Instant.now())
                .build();
    }

    public boolean isCategoryEnabled(NotificationType type) {
        if (type == null) {
            return true;
        }
        return switch (type) {
            case NEW_COMMENT -> commentsEnabled;
            case RANKING_UPDATED -> rankingEnabled;
            case CHALLENGE_COMPLETED -> challengeEnabled;
            case JOIN_REQUEST_APPROVED -> invitationEnabled;
            case STREAK_UPDATED -> streakEnabled;
            default -> true;
        };
    }
}
