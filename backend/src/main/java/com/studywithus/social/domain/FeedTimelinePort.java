package com.studywithus.social.domain;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface FeedTimelinePort {

    void addCheckinToTimelines(UUID checkinId, Instant createdAt, UUID communityId, Collection<UUID> userIds);

    void removeCheckinFromTimelines(UUID checkinId, UUID communityId, Collection<UUID> userIds);

    FeedTimelineSlice getUserTimeline(UUID userId, Instant cursorTime, UUID cursorId, int limit);

    FeedTimelineSlice getCommunityTimeline(UUID communityId, Instant cursorTime, UUID cursorId, int limit);

    void warmUserTimeline(UUID userId, List<FeedTimelineEntry> entries);

    void warmCommunityTimeline(UUID communityId, List<FeedTimelineEntry> entries);

    void invalidateUserTimeline(UUID userId);

    void invalidateCommunityTimeline(UUID communityId);
}
