package com.studywithus.checkin.domain;

import java.time.Instant;
import java.util.UUID;

public record CheckinCreatedEvent(
        UUID checkinId,
        UUID userId,
        UUID communityId,
        Instant occurredAt
) {
    public CheckinCreatedEvent(UUID checkinId, UUID userId, UUID communityId) {
        this(checkinId, userId, communityId, Instant.now());
    }
}
