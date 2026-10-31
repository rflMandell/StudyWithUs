package com.studywithus.community.application;

import com.studywithus.community.domain.CommunityMember;
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
    public static CommunityMemberResult from(CommunityMember member) {
        return new CommunityMemberResult(
                member.getCommunityId(),
                member.getUserId(),
                null,
                null,
                null,
                member.getRole(),
                member.getStatus(),
                member.getJoinedAt()
        );
    }

    public static CommunityMemberResult of(CommunityMember member, String username, String name, String avatarUrl) {
        return new CommunityMemberResult(
                member.getCommunityId(),
                member.getUserId(),
                username,
                name,
                avatarUrl,
                member.getRole(),
                member.getStatus(),
                member.getJoinedAt()
        );
    }
}
