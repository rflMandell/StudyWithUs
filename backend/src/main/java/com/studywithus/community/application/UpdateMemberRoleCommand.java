package com.studywithus.community.application;

import com.studywithus.community.domain.CommunityRole;

import java.util.UUID;

public record UpdateMemberRoleCommand(
        UUID communityId,
        UUID targetUserId,
        UUID actorUserId,
        CommunityRole newRole
) {
}
