package com.studywithus.admin.application;

import com.studywithus.identity.domain.User;

public record AdminUserListItem(
        User user,
        String plan
) {
}
