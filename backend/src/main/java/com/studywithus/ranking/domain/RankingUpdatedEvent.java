package com.studywithus.ranking.domain;

import java.time.Instant;
import java.util.UUID;

public record RankingUpdatedEvent(
        UUID communityId,
        UUID periodId,
        UUID userId,
        long totalPoints,
        int position,
        Instant occurredAt
) {
    public RankingUpdatedEvent(UUID communityId, UUID periodId, UUID userId, long totalPoints, int position) {
        this(communityId, periodId, userId, totalPoints, position, Instant.now());
    }
}
