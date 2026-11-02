package com.studywithus.checkin.application;

import com.studywithus.checkin.domain.Checkin;
import com.studywithus.checkin.domain.CheckinMedia;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public record CheckinResult(
        UUID id,
        UUID userId,
        UUID communityId,
        String caption,
        List<String> mediaKeys,
        Instant createdAt,
        Instant updatedAt
) {
    public static CheckinResult fromDomain(Checkin checkin) {
        List<String> keys = checkin.getMedia() != null
                ? checkin.getMedia().stream().map(CheckinMedia::getStorageKey).toList()
                : Collections.emptyList();

        return new CheckinResult(
                checkin.getId(),
                checkin.getUserId(),
                checkin.getCommunityId(),
                checkin.getCaption(),
                keys,
                checkin.getCreatedAt(),
                checkin.getUpdatedAt()
        );
    }
}
