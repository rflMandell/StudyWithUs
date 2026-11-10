package com.studywithus.social.domain;

import java.time.Instant;
import java.util.UUID;

public record CommentDeletedEvent(
        UUID commentId,
        UUID checkinId,
        UUID authorId,
        UUID communityId,
        Instant occurredAt
) {
    public CommentDeletedEvent(UUID commentId, UUID checkinId, UUID authorId, UUID communityId) {
        this(commentId, checkinId, authorId, communityId, Instant.now());
    }
}
