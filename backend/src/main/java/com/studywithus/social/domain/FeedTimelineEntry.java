package com.studywithus.social.domain;

import java.time.Instant;
import java.util.UUID;

public record FeedTimelineEntry(
        UUID checkinId,
        Instant createdAt
) {
}
