package com.studywithus.social.application;

import java.util.UUID;

public record ListCommentsQuery(
        UUID checkinId,
        int page,
        int size
) {
}
