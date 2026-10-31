package com.studywithus.community.presentation;

import com.studywithus.community.application.CommunityMemberResult;
import com.studywithus.community.domain.CommunityRole;
import com.studywithus.community.domain.MemberStatus;

import java.time.Instant;
import java.util.UUID;

public record CommunityMemberResponse(
        UUID communityId,
        UUID userId,
        String username,
        String name,
        String avatarUrl,
        CommunityRole role,
        MemberStatus status,
        Instant joinedAt
) {
    public static CommunityMemberResponse from(CommunityMemberResult result) {
        return new CommunityMemberResponse(
                result.communityId(),
                result.userId(),
                result.username(),
                result.name(),
                result.avatarUrl(),
                result.role(),
                result.status(),
                result.joinedAt()
        );
    }
}
