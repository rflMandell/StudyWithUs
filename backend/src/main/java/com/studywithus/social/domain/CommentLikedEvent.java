package com.studywithus.social.domain;

import java.time.Instant;
import java.util.UUID;

public record CommentLikedEvent(
        UUID commentId,
        UUID checkinId,
        UUID commentAuthorId,
        UUID likerUserId,
        UUID communityId,
        Instant occurredAt
) {
    public CommentLikedEvent(UUID commentId, UUID checkinId, UUID commentAuthorId, UUID likerUserId, UUID communityId) {
        this(commentId, checkinId, commentAuthorId, likerUserId, communityId, Instant.now());
    }
}
