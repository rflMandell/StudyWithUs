package com.studywithus.community.domain;

import java.time.Instant;
import java.util.UUID;

public record CommunityCreatedEvent(
        UUID communityId,
        String name,
        UUID ownerId,
        Instant occurredAt
) {
    public static CommunityCreatedEvent of(Community community) {
        return new CommunityCreatedEvent(community.getId(), community.getName(), community.getOwnerId(), Instant.now());
    }
}
