package com.studywithus.notification.application;

import com.studywithus.checkin.domain.Checkin;
import com.studywithus.checkin.domain.CheckinRepository;
import com.studywithus.community.domain.CommunityJoinRequestReviewedEvent;
import com.studywithus.community.domain.CommunityJoinRequestStatus;
import com.studywithus.gamification.domain.BadgeGrantedEvent;
import com.studywithus.gamification.domain.ChallengeCompletedEvent;
import com.studywithus.identity.domain.User;
import com.studywithus.identity.domain.UserRepository;
import com.studywithus.notification.domain.NotificationType;
import com.studywithus.social.domain.CommentCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final CreateInAppNotificationUseCase createInAppNotificationUseCase;
    private final CheckinRepository checkinRepository;
    private final UserRepository userRepository;

    @EventListener
    public void onCommentCreated(CommentCreatedEvent event) {
        log.debug("NotificationEventListener received CommentCreatedEvent: commentId={}, checkinId={}",
                event.commentId(), event.checkinId());

        Optional<Checkin> checkinOpt = checkinRepository.findById(event.checkinId());
        if (checkinOpt.isEmpty()) {
            return;
        }

        Checkin checkin = checkinOpt.get();
        // Do not notify if author commented on their own checkin
        if (checkin.getUserId().equals(event.authorId())) {
            return;
        }

        String authorName = userRepository.findById(event.authorId())
                .map(User::getUsername)
                .orElse("Um colega");

        createInAppNotificationUseCase.execute(new CreateNotificationCommand(
                checkin.getUserId(),
                NotificationType.NEW_COMMENT,
                "Novo comentário no seu check-in",
                authorName + " comentou no seu check-in de estudos.",
                "CHECKIN",
                event.checkinId()
        ));
    }

    @EventListener
    public void onBadgeGranted(BadgeGrantedEvent event) {
        log.debug("NotificationEventListener received BadgeGrantedEvent for user: {}, badge: {}",
                event.userId(), event.badgeName());

        createInAppNotificationUseCase.execute(new CreateNotificationCommand(
                event.userId(),
                NotificationType.BADGE_GRANTED,
                "Nova conquista desbloqueada!",
                "Parabéns! Você desbloqueou a badge '" + event.badgeName() + "'!",
                "BADGE",
                event.badgeId()
        ));
    }

    @EventListener
    public void onChallengeCompleted(ChallengeCompletedEvent event) {
        log.debug("NotificationEventListener received ChallengeCompletedEvent for user: {}, challenge: {}",
                event.userId(), event.challengeId());

        createInAppNotificationUseCase.execute(new CreateNotificationCommand(
                event.userId(),
                NotificationType.CHALLENGE_COMPLETED,
                "Desafio concluído com sucesso!",
                "Parabéns por atingir a meta do desafio na plataforma!",
                "CHALLENGE",
                event.challengeId()
        ));
    }

    @EventListener
    public void onCommunityJoinRequestReviewed(CommunityJoinRequestReviewedEvent event) {
        log.debug("NotificationEventListener received CommunityJoinRequestReviewedEvent for user: {}, status: {}",
                event.userId(), event.status());

        if (event.status() == CommunityJoinRequestStatus.APPROVED) {
            createInAppNotificationUseCase.execute(new CreateNotificationCommand(
                    event.userId(),
                    NotificationType.JOIN_REQUEST_APPROVED,
                    "Solicitação de entrada aprovada!",
                    "Sua solicitação para ingressar na comunidade foi aprovada.",
                    "COMMUNITY",
                    event.communityId()
            ));
        }
    }
}
