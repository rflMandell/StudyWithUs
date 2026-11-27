package com.studywithus.social.application;

import java.time.Instant;
import java.util.UUID;

public record PendingFollowRequestResult(
        UUID id,
        UUID requesterId,
        String username,
        String name,
        String avatarUrl,
        Instant createdAt
) {
}
