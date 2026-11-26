package com.studywithus.social.application;

import java.util.UUID;

public record ToggleCommentLikeCommand(
        UUID commentId,
        UUID userId
) {
}
