package com.studywithus.community.domain;

import java.time.Instant;
import java.util.UUID;

public record CommunityMemberRemovedEvent(
        UUID communityId,
        UUID targetUserId,
        UUID removedBy,
        Instant occurredOn
) {
    public CommunityMemberRemovedEvent(UUID communityId, UUID targetUserId, UUID removedBy) {
        this(communityId, targetUserId, removedBy, Instant.now());
    }
}
