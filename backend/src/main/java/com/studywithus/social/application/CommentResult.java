package com.studywithus.social.application;

import com.studywithus.social.domain.Comment;

import java.time.Instant;
import java.util.UUID;

public record CommentResult(
        UUID id,
        UUID checkinId,
        UUID authorId,
        String authorUsername,
        String authorName,
        String authorAvatarUrl,
        String content,
        Instant createdAt,
        Instant updatedAt
) {
    public static CommentResult fromDomain(Comment comment, String authorUsername, String authorName, String authorAvatarUrl) {
        return new CommentResult(
                comment.getId(),
                comment.getCheckinId(),
                comment.getAuthorId(),
                authorUsername,
                authorName,
                authorAvatarUrl,
                comment.getContent(),
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }
}
