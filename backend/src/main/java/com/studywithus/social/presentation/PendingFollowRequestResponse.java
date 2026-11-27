package com.studywithus.social.presentation;

import com.studywithus.social.application.PendingFollowRequestResult;

import java.time.Instant;
import java.util.UUID;

public record PendingFollowRequestResponse(
        UUID id,
        UUID requesterId,
        String username,
        String name,
        String avatarUrl,
        Instant createdAt
) {
    public static PendingFollowRequestResponse from(PendingFollowRequestResult result) {
        return new PendingFollowRequestResponse(
                result.id(),
                result.requesterId(),
                result.username(),
                result.name(),
                result.avatarUrl(),
                result.createdAt()
        );
    }
}
