package com.studywithus.community.application;

import java.util.UUID;

public record JoinCommunityCommand(
        UUID communityId,
        UUID userId
) {
}
