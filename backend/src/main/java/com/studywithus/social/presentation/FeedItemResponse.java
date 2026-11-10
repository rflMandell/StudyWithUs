package com.studywithus.social.presentation;

import com.studywithus.social.application.FeedItemResult;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record FeedItemResponse(
        UUID id,
        UUID userId,
        String authorUsername,
        String authorName,
        String authorAvatarUrl,
        UUID communityId,
        String communityName,
        String caption,
        List<String> mediaKeys,
        long commentsCount,
        Instant createdAt
) {
    public static FeedItemResponse from(FeedItemResult result) {
        return new FeedItemResponse(
                result.id(),
                result.userId(),
                result.authorUsername(),
                result.authorName(),
                result.authorAvatarUrl(),
                result.communityId(),
                result.communityName(),
                result.caption(),
                result.mediaKeys(),
                result.commentsCount(),
                result.createdAt()
        );
    }
}
