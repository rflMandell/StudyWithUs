package com.studywithus.notification.domain;

import java.util.UUID;

public interface RealtimeNotificationPort {
    void sendToUser(UUID userId, RealtimeNotification notification);
    void broadcastToCommunity(UUID communityId, RealtimeNotification notification, UUID excludeUserId);
    void broadcastAll(RealtimeNotification notification);
}
