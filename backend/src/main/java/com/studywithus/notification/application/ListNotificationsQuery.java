package com.studywithus.notification.application;

import java.util.UUID;

public record ListNotificationsQuery(
        UUID userId,
        boolean unreadOnly,
        int page,
        int size
) {
    public ListNotificationsQuery {
        if (page < 0) {
            page = 0;
        }
        if (size <= 0) {
            size = 20;
        }
    }
}
