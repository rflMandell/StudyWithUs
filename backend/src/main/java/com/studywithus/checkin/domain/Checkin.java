package com.studywithus.checkin.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Checkin {
    private UUID id;
    private UUID userId;
    private UUID communityId;
    private String caption;
    @Builder.Default
    private List<CheckinMedia> media = new ArrayList<>();
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;

    public static Checkin create(UUID userId, UUID communityId, String caption, List<String> storageKeys) {
        UUID checkinId = UUID.randomUUID();
        Instant now = Instant.now();

        List<CheckinMedia> mediaList = new ArrayList<>();
        if (storageKeys != null) {
            for (int i = 0; i < storageKeys.size(); i++) {
                mediaList.add(CheckinMedia.create(checkinId, storageKeys.get(i), i));
            }
        }

        return Checkin.builder()
                .id(checkinId)
                .userId(userId)
                .communityId(communityId)
                .caption(caption)
                .media(mediaList)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public boolean isActive() {
        return deletedAt == null;
    }

    public boolean isAuthor(UUID targetUserId) {
        return this.userId != null && this.userId.equals(targetUserId);
    }

    public void delete() {
        Instant now = Instant.now();
        this.deletedAt = now;
        this.updatedAt = now;
    }
}
