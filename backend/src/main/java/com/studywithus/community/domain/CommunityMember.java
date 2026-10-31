package com.studywithus.community.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityMember {
    private UUID communityId;
    private UUID userId;
    private CommunityRole role;
    private MemberStatus status;
    private Instant joinedAt;
    private Instant leftAt;
    private Instant bannedAt;

    public boolean isActive() {
        return status == MemberStatus.ACTIVE;
    }

    public boolean isOwner() {
        return isActive() && role == CommunityRole.OWNER;
    }

    public boolean isAdmin() {
        return isActive() && (role == CommunityRole.OWNER || role == CommunityRole.ADMIN);
    }

    public boolean isModeratorOrHigher() {
        return isActive() && (role == CommunityRole.OWNER || role == CommunityRole.ADMIN || role == CommunityRole.MODERATOR);
    }

    public void leave() {
        this.status = MemberStatus.LEFT;
        this.leftAt = Instant.now();
    }

    public void remove() {
        this.status = MemberStatus.LEFT;
        this.leftAt = Instant.now();
    }

    public void changeRole(CommunityRole newRole) {
        this.role = newRole;
    }
}
