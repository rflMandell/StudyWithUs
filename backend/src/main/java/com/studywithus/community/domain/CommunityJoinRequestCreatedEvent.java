package com.studywithus.community.domain;

import java.time.Instant;
import java.util.UUID;

public record CommunityJoinRequestCreatedEvent(
        UUID requestId,
        UUID communityId,
        UUID userId,
        Instant occurredOn
) {
    public CommunityJoinRequestCreatedEvent(UUID requestId, UUID communityId, UUID userId) {
        this(requestId, communityId, userId, Instant.now());
    }
}
