package com.studywithus.checkin.domain;

import java.time.Instant;
import java.util.UUID;

public record CheckinLikedEvent(
        UUID checkinId,
        UUID checkinAuthorId,
        UUID likerUserId,
        UUID communityId,
        Instant occurredAt
) {
    public CheckinLikedEvent(UUID checkinId, UUID checkinAuthorId, UUID likerUserId, UUID communityId) {
        this(checkinId, checkinAuthorId, likerUserId, communityId, Instant.now());
    }
}
