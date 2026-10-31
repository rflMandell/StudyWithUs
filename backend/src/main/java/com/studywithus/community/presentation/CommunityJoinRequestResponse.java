package com.studywithus.community.presentation;

import com.studywithus.community.application.CommunityJoinRequestResult;
import com.studywithus.community.domain.CommunityJoinRequestStatus;

import java.time.Instant;
import java.util.UUID;

public record CommunityJoinRequestResponse(
        UUID id,
        UUID communityId,
        UUID userId,
        CommunityJoinRequestStatus status,
        Instant createdAt,
        Instant reviewedAt,
        UUID reviewedBy
) {
    public static CommunityJoinRequestResponse from(CommunityJoinRequestResult result) {
        return new CommunityJoinRequestResponse(
                result.id(),
                result.communityId(),
                result.userId(),
                result.status(),
                result.createdAt(),
                result.reviewedAt(),
                result.reviewedBy()
        );
    }
}
