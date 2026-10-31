package com.studywithus.community.application;

import java.util.UUID;

public record LeaveCommunityCommand(
        UUID communityId,
        UUID userId
) {
}
