package com.studywithus.checkin.application;

import java.util.List;
import java.util.UUID;

public record CreateCheckinCommand(
        UUID userId,
        UUID communityId,
        String caption,
        List<String> mediaKeys,
        String idempotencyKey,
        String subject,
        List<String> tags,
        Integer durationMinutes
) {
    public CreateCheckinCommand(
            UUID userId,
            UUID communityId,
            String caption,
            List<String> mediaKeys,
            String idempotencyKey
    ) {
        this(userId, communityId, caption, mediaKeys, idempotencyKey, null, null, null);
    }
}
