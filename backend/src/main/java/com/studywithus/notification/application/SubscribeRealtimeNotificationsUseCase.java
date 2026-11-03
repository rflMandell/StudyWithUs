package com.studywithus.notification.application;

import com.studywithus.notification.infrastructure.SseRealtimeNotificationAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubscribeRealtimeNotificationsUseCase {

    private final SseRealtimeNotificationAdapter sseAdapter;

    public SseEmitter execute(UUID userId) {
        return sseAdapter.subscribe(userId);
    }
}
