package com.studywithus.checkin.application;

import java.util.List;
import java.util.UUID;

public record CreateCheckinCommand(
        UUID userId,
        UUID communityId,
        String caption,
        List<String> mediaKeys,
        String idempotencyKey
) {
}
