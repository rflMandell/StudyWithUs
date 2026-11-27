package com.studywithus.social.presentation;

import com.studywithus.social.application.FollowRelationshipStatus;
import com.studywithus.social.application.FollowStatusResult;

public record FollowStatusResponse(
        FollowRelationshipStatus status,
        long followersCount,
        long followingCount
) {
    public static FollowStatusResponse from(FollowStatusResult result) {
        return new FollowStatusResponse(
                result.status(),
                result.followersCount(),
                result.followingCount()
        );
    }
}
