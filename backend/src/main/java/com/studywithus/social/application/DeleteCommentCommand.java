package com.studywithus.social.application;

import java.util.UUID;

public record DeleteCommentCommand(
        UUID commentId,
        UUID requestingUserId
) {
}
