package com.studywithus.community.application;

import com.studywithus.community.domain.CommunityVisibility;

import java.util.UUID;

public record CreateCommunityCommand(
        String name,
        String description,
        String imageUrl,
        CommunityVisibility visibility,
        String timezone,
        UUID ownerId
) {
}
