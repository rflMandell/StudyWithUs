package com.studywithus.notification.application;

import com.studywithus.checkin.domain.Checkin;
import com.studywithus.checkin.domain.CheckinLikedEvent;
import com.studywithus.checkin.domain.CheckinRepository;
import com.studywithus.community.domain.CommunityJoinRequestReviewedEvent;
import com.studywithus.community.domain.CommunityJoinRequestStatus;
import com.studywithus.gamification.domain.BadgeGrantedEvent;
import com.studywithus.gamification.domain.ChallengeCompletedEvent;
import com.studywithus.gamification.domain.UserLevelUpEvent;
import com.studywithus.identity.domain.User;
import com.studywithus.identity.domain.UserRepository;
import com.studywithus.notification.domain.NotificationType;
import com.studywithus.social.domain.Comment;
import com.studywithus.social.domain.CommentCreatedEvent;
import com.studywithus.social.domain.CommentLikedEvent;
import com.studywithus.social.domain.CommentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final CreateInAppNotificationUseCase createInAppNotificationUseCase;
    private final CheckinRepository checkinRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;

    @EventListener
    public void onCommentCreated(CommentCreatedEvent event) {
        log.debug("NotificationEventListener received CommentCreatedEvent: commentId={}, checkinId={}, parentCommentId={}",
                event.commentId(), event.checkinId(), event.parentCommentId());

        Optional<Checkin> checkinOpt = checkinRepository.findById(event.checkinId());
        if (checkinOpt.isEmpty()) {
            return;
        }

        Checkin checkin = checkinOpt.get();
        UUID notifiedParentAuthorId = null;

        if (event.parentCommentId() != null && commentRepository != null) {
            Optional<Comment> parentOpt = commentRepository.findById(event.parentCommentId());
            if (parentOpt.isPresent()) {
                Comment parentComment = parentOpt.get();
                if (!parentComment.getAuthorId().equals(event.authorId())) {
                    notifiedParentAuthorId = parentComment.getAuthorId();
                    String authorName = resolveUsername(event.authorId());
                    createInAppNotificationUseCase.execute(new CreateNotificationCommand(
                            parentComment.getAuthorId(),
                            NotificationType.COMMENT_REPLY,
                            "Nova resposta ao seu comentário",
                            authorName + " respondeu ao seu comentário.",
                            "COMMENT",
                            event.commentId()
                    ));
                }
            }
        }

        // Notify checkin owner if they are not the comment author and were not already notified as parent comment owner
        if (!checkin.getUserId().equals(event.authorId())
                && !checkin.getUserId().equals(notifiedParentAuthorId)) {
            String authorName = resolveUsername(event.authorId());
            createInAppNotificationUseCase.execute(new CreateNotificationCommand(
                    checkin.getUserId(),
                    NotificationType.NEW_COMMENT,
                    "Novo comentário no seu check-in",
                    authorName + " comentou no seu check-in de estudos.",
                    "CHECKIN",
                    event.checkinId()
            ));
        }
    }

    @EventListener
    public void onCheckinLiked(CheckinLikedEvent event) {
        log.debug("NotificationEventListener received CheckinLikedEvent: checkinId={}, likerUserId={}",
                event.checkinId(), event.likerUserId());

        if (event.checkinAuthorId().equals(event.likerUserId())) {
            return;
        }

        String likerName = resolveUsername(event.likerUserId());
        createInAppNotificationUseCase.execute(new CreateNotificationCommand(
                event.checkinAuthorId(),
                NotificationType.CHECKIN_LIKED,
                "Nova curtida no seu check-in",
                likerName + " curtiu o seu check-in de estudos.",
                "CHECKIN",
                event.checkinId()
        ));
    }

    @EventListener
    public void onCommentLiked(CommentLikedEvent event) {
        log.debug("NotificationEventListener received CommentLikedEvent: commentId={}, likerUserId={}",
                event.commentId(), event.likerUserId());

        if (event.commentAuthorId().equals(event.likerUserId())) {
            return;
        }

        String likerName = resolveUsername(event.likerUserId());
        createInAppNotificationUseCase.execute(new CreateNotificationCommand(
                event.commentAuthorId(),
                NotificationType.COMMENT_LIKED,
                "Nova curtida no seu comentário",
                likerName + " curtiu o seu comentário.",
                "COMMENT",
                event.commentId()
        ));
    }

    private String resolveUsername(UUID userId) {
        return userRepository.findById(userId)
                .map(User::getUsername)
                .orElse("Um colega");
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

    @EventListener
    public void onUserLevelUp(UserLevelUpEvent event) {
        log.debug("NotificationEventListener received UserLevelUpEvent for user: {}, newLevel: {}",
                event.userId(), event.newLevel());

        createInAppNotificationUseCase.execute(new CreateNotificationCommand(
                event.userId(),
                NotificationType.LEVEL_UP,
                "Subiu para o Nível " + event.newLevel() + "!",
                "Parabéns! Você alcançou o nível " + event.newLevel() + " e desbloqueou o título '" + event.unlockedTitle() + "'!",
                "USER",
                event.userId()
        ));
    }
}
