package com.studywithus.checkin.presentation;

import com.studywithus.checkin.application.CheckinResult;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CheckinResponse(
        UUID id,
        UUID userId,
        UUID communityId,
        String caption,
        List<String> mediaKeys,
        Instant createdAt,
        Instant updatedAt
) {
    public static CheckinResponse from(CheckinResult result) {
        return new CheckinResponse(
                result.id(),
                result.userId(),
                result.communityId(),
                result.caption(),
                result.mediaKeys(),
                result.createdAt(),
                result.updatedAt()
        );
    }
}
