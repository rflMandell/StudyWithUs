package com.studywithus.community.application;

import com.studywithus.community.domain.CommunityJoinRequestStatus;

import java.util.UUID;

public record ListJoinRequestsQuery(
        UUID communityId,
        UUID requesterUserId,
        CommunityJoinRequestStatus status,
        int page,
        int size
) {
    public ListJoinRequestsQuery {
        if (status == null) {
            status = CommunityJoinRequestStatus.PENDING;
        }
        if (page < 0) {
            page = 0;
        }
        if (size <= 0 || size > 100) {
            size = 20;
        }
    }
}
