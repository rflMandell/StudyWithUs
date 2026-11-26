package com.studywithus.social.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record FeedItemResult(
        UUID id,
        UUID userId,
        String authorUsername,
        String authorName,
        String authorAvatarUrl,
        int authorLevel,
        String authorTitle,
        String authorAvatarBorder,
        UUID communityId,
        String communityName,
        String caption,
        List<String> mediaKeys,
        long commentsCount,
        long likesCount,
        boolean likedByCurrentUser,
        Instant createdAt
) {
    public FeedItemResult(
            UUID id,
            UUID userId,
            String authorUsername,
            String authorName,
            String authorAvatarUrl,
            int authorLevel,
            String authorTitle,
            String authorAvatarBorder,
            UUID communityId,
            String communityName,
            String caption,
            List<String> mediaKeys,
            long commentsCount,
            Instant createdAt
    ) {
        this(
                id,
                userId,
                authorUsername,
                authorName,
                authorAvatarUrl,
                authorLevel,
                authorTitle,
                authorAvatarBorder,
                communityId,
                communityName,
                caption,
                mediaKeys,
                commentsCount,
                0L,
                false,
                createdAt
        );
    }
}
