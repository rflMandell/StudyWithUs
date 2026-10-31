package com.studywithus.community.application;

import com.studywithus.community.domain.CommunityRole;
import com.studywithus.community.domain.MemberStatus;

import java.time.Instant;
import java.util.UUID;

public record CommunityMemberResult(
        UUID communityId,
        UUID userId,
        String username,
        String name,
        String avatarUrl,
        CommunityRole role,
        MemberStatus status,
        Instant joinedAt
) {
}
