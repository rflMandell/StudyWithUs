package com.studywithus.community.application;

import com.studywithus.community.domain.CommunityJoinRequest;
import com.studywithus.community.domain.CommunityJoinRequestStatus;

import java.time.Instant;
import java.util.UUID;

public record CommunityJoinRequestResult(
        UUID id,
        UUID communityId,
        UUID userId,
        CommunityJoinRequestStatus status,
        Instant createdAt,
        Instant reviewedAt,
        UUID reviewedBy
) {
    public static CommunityJoinRequestResult fromDomain(CommunityJoinRequest request) {
        return new CommunityJoinRequestResult(
                request.getId(),
                request.getCommunityId(),
                request.getUserId(),
                request.getStatus(),
                request.getCreatedAt(),
                request.getReviewedAt(),
                request.getReviewedBy()
        );
    }
}
