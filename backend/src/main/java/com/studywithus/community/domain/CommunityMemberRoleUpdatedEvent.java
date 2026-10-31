package com.studywithus.community.domain;

import java.time.Instant;
import java.util.UUID;

public record CommunityMemberRoleUpdatedEvent(
        UUID communityId,
        UUID targetUserId,
        CommunityRole oldRole,
        CommunityRole newRole,
        UUID updatedBy,
        Instant occurredOn
) {
    public CommunityMemberRoleUpdatedEvent(UUID communityId, UUID targetUserId, CommunityRole oldRole, CommunityRole newRole, UUID updatedBy) {
        this(communityId, targetUserId, oldRole, newRole, updatedBy, Instant.now());
    }
}
