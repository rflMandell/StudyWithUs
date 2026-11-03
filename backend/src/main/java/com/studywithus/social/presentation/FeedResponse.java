package com.studywithus.social.presentation;

import com.studywithus.social.application.FeedCursorResult;

import java.util.List;

public record FeedResponse(
        List<FeedItemResponse> content,
        String nextCursor,
        boolean hasMore
) {
    public static FeedResponse from(FeedCursorResult result) {
        return new FeedResponse(
                result.items().stream().map(FeedItemResponse::from).toList(),
                result.nextCursor(),
                result.hasMore()
        );
    }
}
