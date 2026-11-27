package com.studywithus.identity.application;

import com.studywithus.identity.domain.User;

import java.util.UUID;

public record UserSearchResult(
        UUID id,
        String username,
        String name,
        String avatarUrl,
        boolean isPrivate
) {
    public static UserSearchResult from(User user) {
        return new UserSearchResult(
                user.getId(),
                user.getUsername(),
                user.getName(),
                user.getAvatarUrl(),
                user.isPrivate()
        );
    }
}
