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
        Instant createdAt
) {
}
