package com.studywithus.notification.application;

import com.studywithus.notification.domain.NotificationPreferences;
import com.studywithus.notification.domain.NotificationPreferencesRepository;
import com.studywithus.shared.application.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class UpdateNotificationPreferencesUseCase implements UseCase<UpdateNotificationPreferencesCommand, NotificationPreferencesResult> {

    private final NotificationPreferencesRepository repository;

    @Override
    @Transactional
    public NotificationPreferencesResult execute(UpdateNotificationPreferencesCommand command) {
        NotificationPreferences prefs = repository.findByUserId(command.userId())
                .orElseGet(() -> NotificationPreferences.createDefault(command.userId()));

        if (command.commentsEnabled() != null) {
            prefs.setCommentsEnabled(command.commentsEnabled());
        }
        if (command.rankingEnabled() != null) {
            prefs.setRankingEnabled(command.rankingEnabled());
        }
        if (command.challengeEnabled() != null) {
            prefs.setChallengeEnabled(command.challengeEnabled());
        }
        if (command.invitationEnabled() != null) {
            prefs.setInvitationEnabled(command.invitationEnabled());
        }
        if (command.reminderEnabled() != null) {
            prefs.setReminderEnabled(command.reminderEnabled());
        }
        if (command.streakEnabled() != null) {
            prefs.setStreakEnabled(command.streakEnabled());
        }

        prefs.setUpdatedAt(Instant.now());
        NotificationPreferences saved = repository.save(prefs);

        return NotificationPreferencesResult.fromDomain(saved);
    }
}
