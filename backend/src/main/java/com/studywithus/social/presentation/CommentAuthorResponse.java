package com.studywithus.social.presentation;

import java.util.UUID;

public record CommentAuthorResponse(
        UUID id,
        String username,
        String name,
        String avatarUrl
) {
}
