package com.studywithus.notification.infrastructure;

import com.studywithus.community.domain.CommunityMemberRepository;
import com.studywithus.notification.domain.RealtimeNotification;
import com.studywithus.notification.domain.RealtimeNotificationPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Component
@RequiredArgsConstructor
public class SseRealtimeNotificationAdapter implements RealtimeNotificationPort {

    private static final Long EMITTER_TIMEOUT = 30 * 60 * 1000L; // 30 minutes

    private final CommunityMemberRepository communityMemberRepository;

    private final Map<UUID, List<SseEmitter>> userEmitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(UUID userId) {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT);

        List<SseEmitter> emitters = userEmitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>());
        emitters.add(emitter);

        Runnable cleanup = () -> {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                userEmitters.remove(userId, emitters);
            }
            log.debug("SSE emitter removed for user {}", userId);
        };

        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> cleanup.run());

        // Send an initial handshake event
        try {
            emitter.send(SseEmitter.event()
                    .name("INIT")
                    .data(Map.of("message", "Connected to realtime stream", "userId", userId.toString()), MediaType.APPLICATION_JSON));
        } catch (IOException e) {
            log.warn("Failed to send initial SSE event to user {}: {}", userId, e.getMessage());
            cleanup.run();
        }

        log.info("SSE subscription established for user {}. Active connections for user: {}", userId, emitters.size());
        return emitter;
    }

    @Override
    public void sendToUser(UUID userId, RealtimeNotification notification) {
        List<SseEmitter> emitters = userEmitters.get(userId);
        if (emitters == null || emitters.isEmpty()) {
            log.debug("No active SSE connections for user {}", userId);
            return;
        }

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .id(notification.id().toString())
                        .name(notification.type().name())
                        .data(notification, MediaType.APPLICATION_JSON));
            } catch (IOException | IllegalStateException e) {
                log.debug("Failed sending SSE to user {}, removing dead emitter", userId);
                emitters.remove(emitter);
            }
        }

        if (emitters.isEmpty()) {
            userEmitters.remove(userId, emitters);
        }
    }

    @Override
    public void broadcastToCommunity(UUID communityId, RealtimeNotification notification, UUID excludeUserId) {
        Set<UUID> connectedUsers = userEmitters.keySet();
        for (UUID connectedUser : connectedUsers) {
            if (excludeUserId != null && excludeUserId.equals(connectedUser)) {
                continue;
            }
            try {
                boolean isMember = communityMemberRepository.isUserActiveMember(communityId, connectedUser);
                if (isMember) {
                    sendToUser(connectedUser, notification);
                }
            } catch (Exception e) {
                log.warn("Error checking community membership for user {}: {}", connectedUser, e.getMessage());
            }
        }
    }

    @Override
    public void broadcastAll(RealtimeNotification notification) {
        for (UUID connectedUser : userEmitters.keySet()) {
            sendToUser(connectedUser, notification);
        }
    }

    @Scheduled(fixedRate = 25000)
    public void sendHeartbeat() {
        if (userEmitters.isEmpty()) {
            return;
        }
        for (Map.Entry<UUID, List<SseEmitter>> entry : userEmitters.entrySet()) {
            UUID userId = entry.getKey();
            List<SseEmitter> emitters = entry.getValue();
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event().comment("ping"));
                } catch (IOException | IllegalStateException e) {
                    emitters.remove(emitter);
                }
            }
            if (emitters.isEmpty()) {
                userEmitters.remove(userId, emitters);
            }
        }
    }

    public int getConnectedUserCount() {
        return userEmitters.size();
    }
}
