package com.studywithus.notification.application;

import com.studywithus.checkin.domain.Checkin;
import com.studywithus.checkin.domain.CheckinCreatedEvent;
import com.studywithus.checkin.domain.CheckinLikedEvent;
import com.studywithus.checkin.domain.CheckinRepository;
import com.studywithus.community.domain.CommunityMemberJoinedEvent;
import com.studywithus.gamification.domain.PointsAwardedEvent;
import com.studywithus.gamification.domain.PointsReversedEvent;
import com.studywithus.gamification.domain.StreakUpdatedEvent;
import com.studywithus.gamification.domain.UserLevelUpEvent;
import com.studywithus.notification.domain.NotificationType;
import com.studywithus.notification.domain.RealtimeNotification;
import com.studywithus.notification.domain.RealtimeNotificationPort;
import com.studywithus.ranking.domain.RankingUpdatedEvent;
import com.studywithus.social.domain.Comment;
import com.studywithus.social.domain.CommentCreatedEvent;
import com.studywithus.social.domain.CommentLikedEvent;
import com.studywithus.social.domain.CommentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
public class RealtimeNotificationEventListener {

    private final RealtimeNotificationPort notificationPort;
    private final CheckinRepository checkinRepository;
    private final CommentRepository commentRepository;

    @Autowired
    public RealtimeNotificationEventListener(
            RealtimeNotificationPort notificationPort,
            @Autowired(required = false) CheckinRepository checkinRepository,
            @Autowired(required = false) CommentRepository commentRepository
    ) {
        this.notificationPort = notificationPort;
        this.checkinRepository = checkinRepository;
        this.commentRepository = commentRepository;
    }

    public RealtimeNotificationEventListener(RealtimeNotificationPort notificationPort) {
        this(notificationPort, null, null);
    }

    @EventListener
    public void onCheckinCreated(CheckinCreatedEvent event) {
        log.info("Dispatching realtime notification for checkin created: checkinId={}, communityId={}", event.checkinId(), event.communityId());

        RealtimeNotification notification = RealtimeNotification.create(
                NotificationType.NEW_CHECKIN,
                null,
                event.communityId(),
                "Nova atividade na comunidade",
                "Um participante publicou um novo check-in de estudos.",
                Map.of(
                        "checkinId", event.checkinId().toString(),
                        "authorUserId", event.userId().toString(),
                        "communityId", event.communityId().toString()
                )
        );

        notificationPort.broadcastToCommunity(event.communityId(), notification, event.userId());
    }

    @EventListener
    public void onCheckinLiked(CheckinLikedEvent event) {
        if (event.checkinAuthorId().equals(event.likerUserId())) {
            return;
        }

        RealtimeNotification notification = RealtimeNotification.create(
                NotificationType.CHECKIN_LIKED,
                event.checkinAuthorId(),
                event.communityId(),
                "Nova curtida no seu check-in",
                "Alguém curtiu o seu check-in de estudos.",
                Map.of(
                        "checkinId", event.checkinId().toString(),
                        "likerUserId", event.likerUserId().toString(),
                        "communityId", event.communityId().toString()
                )
        );

        notificationPort.sendToUser(event.checkinAuthorId(), notification);
    }

    @EventListener
    public void onCommentLiked(CommentLikedEvent event) {
        if (event.commentAuthorId().equals(event.likerUserId())) {
            return;
        }

        RealtimeNotification notification = RealtimeNotification.create(
                NotificationType.COMMENT_LIKED,
                event.commentAuthorId(),
                event.communityId(),
                "Nova curtida no seu comentário",
                "Alguém curtiu o seu comentário.",
                Map.of(
                        "commentId", event.commentId().toString(),
                        "checkinId", event.checkinId().toString(),
                        "likerUserId", event.likerUserId().toString(),
                        "communityId", event.communityId().toString()
                )
        );

        notificationPort.sendToUser(event.commentAuthorId(), notification);
    }

    @EventListener
    public void onCommentCreated(CommentCreatedEvent event) {
        if (checkinRepository == null) {
            return;
        }

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
                    RealtimeNotification replyNotification = RealtimeNotification.create(
                            NotificationType.COMMENT_REPLY,
                            parentComment.getAuthorId(),
                            event.communityId(),
                            "Nova resposta ao seu comentário",
                            "Alguém respondeu ao seu comentário.",
                            Map.of(
                                    "commentId", event.commentId().toString(),
                                    "parentCommentId", event.parentCommentId().toString(),
                                    "checkinId", event.checkinId().toString(),
                                    "authorId", event.authorId().toString()
                            )
                    );
                    notificationPort.sendToUser(parentComment.getAuthorId(), replyNotification);
                }
            }
        }

        if (!checkin.getUserId().equals(event.authorId())
                && !checkin.getUserId().equals(notifiedParentAuthorId)) {
            RealtimeNotification commentNotification = RealtimeNotification.create(
                    NotificationType.NEW_COMMENT,
                    checkin.getUserId(),
                    event.communityId(),
                    "Novo comentário no seu check-in",
                    "Alguém comentou no seu check-in de estudos.",
                    Map.of(
                            "commentId", event.commentId().toString(),
                            "checkinId", event.checkinId().toString(),
                            "authorId", event.authorId().toString()
                    )
            );
            notificationPort.sendToUser(checkin.getUserId(), commentNotification);
        }
    }

    @EventListener
    public void onRankingUpdated(RankingUpdatedEvent event) {
        log.info("Dispatching realtime notification for ranking updated: communityId={}, userId={}, position={}",
                event.communityId(), event.userId(), event.position());

        RealtimeNotification notification = RealtimeNotification.create(
                NotificationType.RANKING_UPDATED,
                null,
                event.communityId(),
                "Ranking atualizado",
                "A classificação da comunidade foi atualizada.",
                Map.of(
                        "communityId", event.communityId().toString(),
                        "periodId", event.periodId().toString(),
                        "userId", event.userId().toString(),
                        "totalPoints", event.totalPoints(),
                        "position", event.position()
                )
        );

        notificationPort.broadcastToCommunity(event.communityId(), notification, null);
    }

    @EventListener
    public void onStreakUpdated(StreakUpdatedEvent event) {
        log.info("Dispatching realtime notification for streak updated: userId={}, streak={}",
                event.userId(), event.userCurrentStreak());

        RealtimeNotification notification = RealtimeNotification.create(
                NotificationType.STREAK_UPDATED,
                event.userId(),
                event.communityId(),
                "Sequência atualizada!",
                "Sua sequência agora é de " + event.userCurrentStreak() + " " + (event.userCurrentStreak() == 1 ? "dia" : "dias") + "!",
                Map.of(
                        "userId", event.userId().toString(),
                        "communityId", event.communityId() != null ? event.communityId().toString() : "",
                        "userCurrentStreak", event.userCurrentStreak(),
                        "communityCurrentStreak", event.communityCurrentStreak()
                )
        );

        notificationPort.sendToUser(event.userId(), notification);
    }

    @EventListener
    public void onMemberJoined(CommunityMemberJoinedEvent event) {
        log.info("Dispatching realtime notification for member joined: communityId={}, userId={}",
                event.communityId(), event.userId());

        RealtimeNotification notification = RealtimeNotification.create(
                NotificationType.MEMBER_JOINED,
                null,
                event.communityId(),
                "Novo membro na comunidade",
                "Um novo participante ingressou na comunidade.",
                Map.of(
                        "communityId", event.communityId().toString(),
                        "joinedUserId", event.userId().toString()
                )
        );

        notificationPort.broadcastToCommunity(event.communityId(), notification, event.userId());
    }

    @EventListener
    public void onUserLevelUp(UserLevelUpEvent event) {
        log.info("Dispatching realtime notification for user level up: userId={}, newLevel={}",
                event.userId(), event.newLevel());

        RealtimeNotification notification = RealtimeNotification.create(
                NotificationType.LEVEL_UP,
                event.userId(),
                null,
                "Subiu para o Nível " + event.newLevel() + "!",
                "Parabéns! Você alcançou o nível " + event.newLevel() + " (" + event.unlockedTitle() + ")!",
                Map.of(
                        "userId", event.userId().toString(),
                        "previousLevel", event.previousLevel(),
                        "newLevel", event.newLevel(),
                        "totalXp", event.totalXp(),
                        "unlockedTitle", event.unlockedTitle(),
                        "unlockedAvatarBorder", event.unlockedAvatarBorder()
                )
        );

        notificationPort.sendToUser(event.userId(), notification);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onPointsAwarded(PointsAwardedEvent event) {
        log.info("Dispatching realtime notification for points awarded: userId={}, amount={}, type={}",
                event.userId(), event.amount(), event.type());

        RealtimeNotification notification = RealtimeNotification.create(
                NotificationType.POINTS_AWARDED,
                event.userId(),
                event.communityId(),
                "Pontos recebidos!",
                "Você ganhou +" + event.amount() + " pontos!",
                Map.of(
                        "transactionId", event.transactionId().toString(),
                        "userId", event.userId().toString(),
                        "communityId", event.communityId() != null ? event.communityId().toString() : "",
                        "amount", event.amount(),
                        "type", event.type().name()
                )
        );

        notificationPort.sendToUser(event.userId(), notification);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onPointsReversed(PointsReversedEvent event) {
        log.info("Dispatching realtime notification for points reversed: userId={}, amount={}",
                event.userId(), event.amount());

        RealtimeNotification notification = RealtimeNotification.create(
                NotificationType.POINTS_AWARDED,
                event.userId(),
                event.communityId(),
                "Pontuação atualizada",
                "Estorno de " + Math.abs(event.amount()) + " pontos.",
                Map.of(
                        "reversalTransactionId", event.reversalTransactionId().toString(),
                        "originalTransactionId", event.originalTransactionId().toString(),
                        "userId", event.userId().toString(),
                        "communityId", event.communityId() != null ? event.communityId().toString() : "",
                        "amount", event.amount()
                )
        );

        notificationPort.sendToUser(event.userId(), notification);
    }
}
