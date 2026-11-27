package com.studywithus.identity.presentation;

import com.studywithus.identity.application.UserSearchResult;

import java.util.UUID;

public record UserSearchItemResponse(
        UUID id,
        String username,
        String name,
        String avatarUrl,
        boolean isPrivate
) {
    public static UserSearchItemResponse from(UserSearchResult result) {
        return new UserSearchItemResponse(
                result.id(),
                result.username(),
                result.name(),
                result.avatarUrl(),
                result.isPrivate()
        );
    }
}
