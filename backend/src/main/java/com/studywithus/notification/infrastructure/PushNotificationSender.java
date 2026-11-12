package com.studywithus.notification.infrastructure;

import com.studywithus.notification.domain.DeviceToken;
import com.studywithus.notification.domain.DeviceTokenRepository;
import com.studywithus.notification.domain.PushNotificationPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PushNotificationSender implements PushNotificationPort {

    private final DeviceTokenRepository deviceTokenRepository;

    @Override
    @Async
    public void sendPushNotification(UUID userId, String title, String body, Map<String, String> data) {
        try {
            List<DeviceToken> activeTokens = deviceTokenRepository.findActiveTokensByUserId(userId);
            if (activeTokens.isEmpty()) {
                log.debug("No active device tokens found for user: {}", userId);
                return;
            }

            for (DeviceToken token : activeTokens) {
                log.info("Dispatched push notification to user {} on platform {} [tokenPrefix={}] title='{}'",
                        userId, token.getPlatform(),
                        token.getToken().substring(0, Math.min(8, token.getToken().length())),
                        title);
                // Simulated gateway call (FCM / APNs)
            }
        } catch (Exception ex) {
            log.error("Failed to send push notification to user {}: {}", userId, ex.getMessage(), ex);
        }
    }
}
