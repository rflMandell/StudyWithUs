package com.studywithus.notification.presentation;

import com.studywithus.identity.infrastructure.SecurityUserPrincipal;
import com.studywithus.notification.application.SubscribeRealtimeNotificationsUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@RestController
@RequestMapping("/api/v1/realtime")
@RequiredArgsConstructor
public class RealtimeNotificationController {

    private final SubscribeRealtimeNotificationsUseCase subscribeUseCase;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@AuthenticationPrincipal SecurityUserPrincipal principal) {
        log.info("Client subscribing to realtime SSE stream: userId={}", principal.getId());
        return subscribeUseCase.execute(principal.getId());
    }
}
