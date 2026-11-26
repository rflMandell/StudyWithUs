package com.studywithus.social.application;

import com.studywithus.social.domain.Comment;

import java.time.Instant;
import java.util.UUID;

public record CommentResult(
        UUID id,
        UUID checkinId,
        UUID parentCommentId,
        UUID authorId,
        String authorUsername,
        String authorName,
        String authorAvatarUrl,
        String content,
        long likesCount,
        boolean likedByCurrentUser,
        Instant createdAt,
        Instant updatedAt
) {
    public static CommentResult fromDomain(Comment comment, String authorUsername, String authorName, String authorAvatarUrl) {
        return fromDomain(comment, authorUsername, authorName, authorAvatarUrl, 0L, false);
    }

    public static CommentResult fromDomain(
            Comment comment,
            String authorUsername,
            String authorName,
            String authorAvatarUrl,
            long likesCount,
            boolean likedByCurrentUser
    ) {
        return new CommentResult(
                comment.getId(),
                comment.getCheckinId(),
                comment.getParentCommentId(),
                comment.getAuthorId(),
                authorUsername,
                authorName,
                authorAvatarUrl,
                comment.getContent(),
                likesCount,
                likedByCurrentUser,
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }
}
