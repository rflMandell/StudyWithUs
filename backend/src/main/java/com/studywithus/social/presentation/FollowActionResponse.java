package com.studywithus.social.presentation;

import com.studywithus.social.application.FollowActionResult;
import com.studywithus.social.application.FollowRelationshipStatus;

public record FollowActionResponse(FollowRelationshipStatus status) {
    public static FollowActionResponse from(FollowActionResult result) {
        return new FollowActionResponse(result.status());
    }
}
