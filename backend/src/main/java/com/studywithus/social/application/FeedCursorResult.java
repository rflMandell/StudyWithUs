package com.studywithus.social.application;

import java.util.List;

public record FeedCursorResult(
        List<FeedItemResult> items,
        String nextCursor,
        boolean hasMore
) {
}
