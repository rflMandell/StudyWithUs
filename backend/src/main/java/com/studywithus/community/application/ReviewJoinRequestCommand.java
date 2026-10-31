package com.studywithus.community.application;

import java.util.UUID;

public record ReviewJoinRequestCommand(
        UUID communityId,
        UUID requestId,
        UUID reviewerUserId,
        boolean approved
) {
}
