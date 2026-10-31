package com.studywithus.community.application;

import java.util.UUID;

public record RequestJoinCommunityCommand(
        UUID communityId,
        UUID userId
) {
}
