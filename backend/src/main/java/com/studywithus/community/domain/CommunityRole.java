package com.studywithus.community.domain;

public enum CommunityRole {
    OWNER(4),
    ADMIN(3),
    MODERATOR(2),
    MEMBER(1);

    private final int hierarchyLevel;

    CommunityRole(int hierarchyLevel) {
        this.hierarchyLevel = hierarchyLevel;
    }

    public int getHierarchyLevel() {
        return hierarchyLevel;
    }

    public boolean canManage(CommunityRole targetRole) {
        if (this == OWNER) {
            return targetRole != OWNER;
        }
        if (this == ADMIN) {
            return targetRole == MODERATOR || targetRole == MEMBER;
        }
        return false;
    }
}
