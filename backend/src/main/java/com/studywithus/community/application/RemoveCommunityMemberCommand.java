package com.studywithus.community.application;

import java.util.UUID;

public record RemoveCommunityMemberCommand(
        UUID communityId,
        UUID targetUserId,
        UUID actorUserId
) {
}
