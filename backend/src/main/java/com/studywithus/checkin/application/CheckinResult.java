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
        long commentsCount,
        Instant createdAt,
        Instant updatedAt
) {
    public static CheckinResult fromDomain(Checkin checkin) {
        return fromDomain(checkin, 0L);
    }

    public static CheckinResult fromDomain(Checkin checkin, long commentsCount) {
        List<String> keys = checkin.getMedia() != null
                ? checkin.getMedia().stream().map(CheckinMedia::getStorageKey).toList()
                : Collections.emptyList();

        return new CheckinResult(
                checkin.getId(),
                checkin.getUserId(),
                checkin.getCommunityId(),
                checkin.getCaption(),
                keys,
                commentsCount,
                checkin.getCreatedAt(),
                checkin.getUpdatedAt()
        );
    }
}
