package com.studywithus.community.presentation;

import com.studywithus.community.domain.CommunityRole;
import jakarta.validation.constraints.NotNull;

public record UpdateMemberRoleRequest(
        @NotNull(message = "Role is required")
        CommunityRole role
) {
}
