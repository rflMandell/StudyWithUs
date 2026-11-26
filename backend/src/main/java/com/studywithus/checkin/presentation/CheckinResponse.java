package com.studywithus.checkin.presentation;

import com.studywithus.checkin.application.CheckinResult;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

public record CheckinResponse(
        UUID id,
        UUID userId,
        UUID communityId,
        String caption,
        List<String> mediaKeys,
        List<String> mediaUrls,
        long commentsCount,
        long likesCount,
        boolean likedByCurrentUser,
        Instant createdAt,
        Instant updatedAt
) {
    public static CheckinResponse from(CheckinResult result) {
        return from(result, null);
    }

    public static CheckinResponse from(CheckinResult result, Function<String, String> urlResolver) {
        List<String> urls = (urlResolver != null && result.mediaKeys() != null)
                ? result.mediaKeys().stream().map(urlResolver).toList()
                : Collections.emptyList();
        return new CheckinResponse(
                result.id(),
                result.userId(),
                result.communityId(),
                result.caption(),
                result.mediaKeys(),
                urls,
                result.commentsCount(),
                result.likesCount(),
                result.likedByCurrentUser(),
                result.createdAt(),
                result.updatedAt()
        );
    }
}
