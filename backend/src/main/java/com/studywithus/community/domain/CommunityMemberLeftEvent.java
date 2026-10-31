package com.studywithus.community.domain;

import java.time.Instant;
import java.util.UUID;

public record CommunityMemberLeftEvent(
        UUID communityId,
        UUID userId,
        Instant occurredAt
) {
    public static CommunityMemberLeftEvent of(UUID communityId, UUID userId) {
        return new CommunityMemberLeftEvent(communityId, userId, Instant.now());
    }
}
