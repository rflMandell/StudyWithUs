package com.studywithus.community.domain;

import java.time.Instant;
import java.util.UUID;

public record CommunityMemberJoinedEvent(
        UUID communityId,
        UUID userId,
        CommunityRole role,
        Instant occurredAt
) {
    public static CommunityMemberJoinedEvent of(CommunityMember member) {
        return new CommunityMemberJoinedEvent(member.getCommunityId(), member.getUserId(), member.getRole(), Instant.now());
    }
}
