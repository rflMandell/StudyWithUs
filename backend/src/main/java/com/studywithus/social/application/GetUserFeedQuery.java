package com.studywithus.social.application;

import java.util.UUID;

public record GetUserFeedQuery(
        UUID userId,
        String cursor,
        int limit
) {
}
