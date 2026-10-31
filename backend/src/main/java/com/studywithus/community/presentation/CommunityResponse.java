package com.studywithus.community.presentation;

import com.studywithus.community.application.CommunityResult;
import com.studywithus.community.domain.CommunityRole;
import com.studywithus.community.domain.CommunityVisibility;

import java.time.Instant;
import java.util.UUID;

public record CommunityResponse(
        UUID id,
        String name,
        String description,
        String imageUrl,
        CommunityVisibility visibility,
        UUID ownerId,
        String rankingType,
        String timezone,
        long membersCount,
        CommunityRole currentUserRole,
        Instant createdAt
) {
    public static CommunityResponse from(CommunityResult result) {
        return new CommunityResponse(
                result.id(),
                result.name(),
                result.description(),
                result.imageUrl(),
                result.visibility(),
                result.ownerId(),
                result.rankingType(),
                result.timezone(),
                result.membersCount(),
                result.currentUserRole(),
                result.createdAt()
        );
    }
}
