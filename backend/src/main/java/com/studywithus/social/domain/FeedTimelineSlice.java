package com.studywithus.social.domain;

import java.util.List;
import java.util.UUID;

public record FeedTimelineSlice(
        boolean cacheHit,
        List<UUID> checkinIds
) {
    public static FeedTimelineSlice miss() {
        return new FeedTimelineSlice(false, List.of());
    }

    public static FeedTimelineSlice hit(List<UUID> checkinIds) {
        return new FeedTimelineSlice(true, checkinIds != null ? checkinIds : List.of());
    }
}
