package com.studywithus.social.application;

public record FollowStatusResult(
        FollowRelationshipStatus status,
        long followersCount,
        long followingCount
) {
}
