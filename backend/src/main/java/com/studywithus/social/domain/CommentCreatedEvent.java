package com.studywithus.social.domain;

import java.time.Instant;
import java.util.UUID;

public record CommentCreatedEvent(
        UUID commentId,
        UUID checkinId,
        UUID parentCommentId,
        UUID authorId,
        UUID communityId,
        Instant occurredAt
) {
    public CommentCreatedEvent(UUID commentId, UUID checkinId, UUID authorId, UUID communityId) {
        this(commentId, checkinId, null, authorId, communityId, Instant.now());
    }

    public CommentCreatedEvent(UUID commentId, UUID checkinId, UUID parentCommentId, UUID authorId, UUID communityId) {
        this(commentId, checkinId, parentCommentId, authorId, communityId, Instant.now());
    }
}
