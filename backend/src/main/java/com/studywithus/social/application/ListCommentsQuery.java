package com.studywithus.social.application;

import java.util.UUID;

public record ListCommentsQuery(
        UUID checkinId,
        UUID callerId,
        int page,
        int size
) {
    public ListCommentsQuery(UUID checkinId, int page, int size) {
        this(checkinId, null, page, size);
    }
}
