package com.studywithus.notification.infrastructure;

import com.studywithus.notification.domain.NotificationPreferences;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification_preferences")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreferencesJpaEntity {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "comments_enabled", nullable = false)
    private boolean commentsEnabled;

    @Column(name = "ranking_enabled", nullable = false)
    private boolean rankingEnabled;

    @Column(name = "challenge_enabled", nullable = false)
    private boolean challengeEnabled;

    @Column(name = "invitation_enabled", nullable = false)
    private boolean invitationEnabled;

    @Column(name = "reminder_enabled", nullable = false)
    private boolean reminderEnabled;

    @Column(name = "streak_enabled", nullable = false)
    private boolean streakEnabled;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static NotificationPreferencesJpaEntity fromDomain(NotificationPreferences domain) {
        return NotificationPreferencesJpaEntity.builder()
                .userId(domain.getUserId())
                .commentsEnabled(domain.isCommentsEnabled())
                .rankingEnabled(domain.isRankingEnabled())
                .challengeEnabled(domain.isChallengeEnabled())
                .invitationEnabled(domain.isInvitationEnabled())
                .reminderEnabled(domain.isReminderEnabled())
                .streakEnabled(domain.isStreakEnabled())
                .updatedAt(domain.getUpdatedAt() != null ? domain.getUpdatedAt() : Instant.now())
                .build();
    }

    public NotificationPreferences toDomain() {
        return NotificationPreferences.builder()
                .userId(this.userId)
                .commentsEnabled(this.commentsEnabled)
                .rankingEnabled(this.rankingEnabled)
                .challengeEnabled(this.challengeEnabled)
                .invitationEnabled(this.invitationEnabled)
                .reminderEnabled(this.reminderEnabled)
                .streakEnabled(this.streakEnabled)
                .updatedAt(this.updatedAt)
                .build();
    }
}
