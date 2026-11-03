package com.studywithus.notification.application;

import com.studywithus.checkin.domain.CheckinCreatedEvent;
import com.studywithus.community.domain.CommunityMemberJoinedEvent;
import com.studywithus.gamification.domain.StreakUpdatedEvent;
import com.studywithus.notification.domain.NotificationType;
import com.studywithus.notification.domain.RealtimeNotification;
import com.studywithus.notification.domain.RealtimeNotificationPort;
import com.studywithus.ranking.domain.RankingUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class RealtimeNotificationEventListener {

    private final RealtimeNotificationPort notificationPort;

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
}
