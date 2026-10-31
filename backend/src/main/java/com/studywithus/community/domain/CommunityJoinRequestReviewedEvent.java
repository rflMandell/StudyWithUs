package com.studywithus.community.domain;

import java.time.Instant;
import java.util.UUID;

public record CommunityJoinRequestReviewedEvent(
        UUID requestId,
        UUID communityId,
        UUID userId,
        UUID reviewedBy,
        CommunityJoinRequestStatus status,
        Instant occurredOn
) {
    public CommunityJoinRequestReviewedEvent(UUID requestId, UUID communityId, UUID userId, UUID reviewedBy, CommunityJoinRequestStatus status) {
        this(requestId, communityId, userId, reviewedBy, status, Instant.now());
    }
}
