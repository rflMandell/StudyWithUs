package com.studywithus.social.application;

import java.util.UUID;

public record CreateCommentCommand(
        UUID checkinId,
        UUID authorId,
        String content,
        UUID parentCommentId
) {
    public CreateCommentCommand(UUID checkinId, UUID authorId, String content) {
        this(checkinId, authorId, content, null);
    }
}
