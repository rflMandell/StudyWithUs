package com.studywithus.social.presentation;

import com.studywithus.social.application.FeedCursorResult;

import java.util.List;
import java.util.function.Function;

public record FeedResponse(
        List<FeedItemResponse> content,
        String nextCursor,
        boolean hasMore
) {
    public static FeedResponse from(FeedCursorResult result) {
        return from(result, null);
    }

    public static FeedResponse from(FeedCursorResult result, Function<String, String> urlResolver) {
        return new FeedResponse(
                result.items().stream().map(item -> FeedItemResponse.from(item, urlResolver)).toList(),
                result.nextCursor(),
                result.hasMore()
        );
    }
}
