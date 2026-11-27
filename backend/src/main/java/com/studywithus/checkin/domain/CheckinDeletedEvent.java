package com.studywithus.checkin.domain;

import java.time.Instant;
import java.util.UUID;

public record CheckinDeletedEvent(
        UUID checkinId,
        UUID userId,
        UUID communityId,
        UUID deletedBy,
        Instant occurredAt,
        boolean countsAsCheckin
) {
    public CheckinDeletedEvent(UUID checkinId, UUID userId, UUID communityId, UUID deletedBy) {
        this(checkinId, userId, communityId, deletedBy, Instant.now(), true);
    }

    public CheckinDeletedEvent(UUID checkinId, UUID userId, UUID communityId, UUID deletedBy, boolean countsAsCheckin) {
        this(checkinId, userId, communityId, deletedBy, Instant.now(), countsAsCheckin);
    }

    public CheckinDeletedEvent(UUID checkinId, UUID userId, UUID communityId, UUID deletedBy, Instant occurredAt) {
        this(checkinId, userId, communityId, deletedBy, occurredAt, true);
    }
}
