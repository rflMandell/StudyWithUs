package com.studywithus.checkin.domain;

import java.time.Instant;
import java.util.UUID;

public record CheckinDeletedEvent(
        UUID checkinId,
        UUID userId,
        UUID communityId,
        UUID deletedBy,
        Instant occurredAt
) {
    public CheckinDeletedEvent(UUID checkinId, UUID userId, UUID communityId, UUID deletedBy) {
        this(checkinId, userId, communityId, deletedBy, Instant.now());
    }
}
