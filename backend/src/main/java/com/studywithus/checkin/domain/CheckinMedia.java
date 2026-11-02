package com.studywithus.checkin.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckinMedia {
    private UUID id;
    private UUID checkinId;
    private String storageKey;
    private int position;
    private Instant createdAt;

    public static CheckinMedia create(UUID checkinId, String storageKey, int position) {
        return CheckinMedia.builder()
                .id(UUID.randomUUID())
                .checkinId(checkinId)
                .storageKey(storageKey)
                .position(position)
                .createdAt(Instant.now())
                .build();
    }
}
