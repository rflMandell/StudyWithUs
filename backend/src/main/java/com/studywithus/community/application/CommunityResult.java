package com.studywithus.community.application;

import com.studywithus.community.domain.Community;
import com.studywithus.community.domain.CommunityRole;
import com.studywithus.community.domain.CommunityVisibility;

import java.time.Instant;
import java.util.UUID;

public record CommunityResult(
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
    public static CommunityResult from(Community community, long membersCount, CommunityRole currentUserRole) {
        return new CommunityResult(
                community.getId(),
                community.getName(),
                community.getDescription(),
                community.getImageUrl(),
                community.getVisibility(),
                community.getOwnerId(),
                community.getRankingType(),
                community.getTimezone(),
                membersCount,
                currentUserRole,
                community.getCreatedAt()
        );
    }
}
