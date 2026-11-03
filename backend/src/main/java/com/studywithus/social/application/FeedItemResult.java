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
        UUID communityId,
        String communityName,
        String caption,
        List<String> mediaKeys,
        Instant createdAt
) {
}
