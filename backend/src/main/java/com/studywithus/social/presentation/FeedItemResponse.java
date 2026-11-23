package com.studywithus.social.presentation;

import com.studywithus.social.application.FeedItemResult;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

public record FeedItemResponse(
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
        List<String> mediaUrls,
        long commentsCount,
        Instant createdAt
) {
    public static FeedItemResponse from(FeedItemResult result) {
        return from(result, null);
    }

    public static FeedItemResponse from(FeedItemResult result, Function<String, String> urlResolver) {
        List<String> urls = (urlResolver != null && result.mediaKeys() != null)
                ? result.mediaKeys().stream().map(urlResolver).toList()
                : Collections.emptyList();
        return new FeedItemResponse(
                result.id(),
                result.userId(),
                result.authorUsername(),
                result.authorName(),
                result.authorAvatarUrl(),
                result.authorLevel(),
                result.authorTitle(),
                result.authorAvatarBorder(),
                result.communityId(),
                result.communityName(),
                result.caption(),
                result.mediaKeys(),
                urls,
                result.commentsCount(),
                result.createdAt()
        );
    }
}
