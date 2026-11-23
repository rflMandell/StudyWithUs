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
    private String subject;
    @Builder.Default
    private List<String> tags = new ArrayList<>();
    private int durationMinutes;
    @Builder.Default
    private List<CheckinMedia> media = new ArrayList<>();
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;

    public static Checkin create(UUID userId, UUID communityId, String caption, List<String> storageKeys) {
        return create(userId, communityId, caption, storageKeys, null, null, null);
    }

    public static Checkin create(
            UUID userId,
            UUID communityId,
            String caption,
            List<String> storageKeys,
            String subject,
            List<String> tags,
            Integer durationMinutes
    ) {
        UUID checkinId = UUID.randomUUID();
        Instant now = Instant.now();

        List<CheckinMedia> mediaList = new ArrayList<>();
        if (storageKeys != null) {
            for (int i = 0; i < storageKeys.size(); i++) {
                mediaList.add(CheckinMedia.create(checkinId, storageKeys.get(i), i));
            }
        }

        List<String> safeTags = tags != null
                ? tags.stream().filter(t -> t != null && !t.isBlank()).map(String::trim).toList()
                : new ArrayList<>();
        int safeDuration = durationMinutes != null && durationMinutes > 0 ? durationMinutes : 0;

        return Checkin.builder()
                .id(checkinId)
                .userId(userId)
                .communityId(communityId)
                .caption(caption)
                .subject(subject != null && !subject.isBlank() ? subject.trim() : null)
                .tags(safeTags)
                .durationMinutes(safeDuration)
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
