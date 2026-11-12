package com.studywithus.notification.domain;

import java.util.Map;
import java.util.UUID;

public interface PushNotificationPort {
    void sendPushNotification(UUID userId, String title, String body, Map<String, String> data);
}
