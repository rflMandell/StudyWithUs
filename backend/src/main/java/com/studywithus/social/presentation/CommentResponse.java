package com.studywithus.social.presentation;

import com.studywithus.social.application.CommentResult;

import java.time.Instant;
import java.util.UUID;

public record CommentResponse(
        UUID id,
        UUID checkinId,
        UUID parentCommentId,
        CommentAuthorResponse author,
        String content,
        long likesCount,
        boolean likedByCurrentUser,
        Instant createdAt,
        Instant updatedAt
) {
    public static CommentResponse from(CommentResult result) {
        return new CommentResponse(
                result.id(),
                result.checkinId(),
                result.parentCommentId(),
                new CommentAuthorResponse(
                        result.authorId(),
                        result.authorUsername(),
                        result.authorName(),
                        result.authorAvatarUrl()
                ),
                result.content(),
                result.likesCount(),
                result.likedByCurrentUser(),
                result.createdAt(),
                result.updatedAt()
        );
    }
}
