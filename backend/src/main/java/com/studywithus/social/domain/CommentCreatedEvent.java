package com.studywithus.social.domain;

import java.time.Instant;
import java.util.UUID;

public record CommentCreatedEvent(
        UUID commentId,
        UUID checkinId,
        UUID authorId,
        UUID communityId,
        Instant occurredAt
) {
    public CommentCreatedEvent(UUID commentId, UUID checkinId, UUID authorId, UUID communityId) {
        this(commentId, checkinId, authorId, communityId, Instant.now());
    }
}
