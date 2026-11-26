package com.studywithus.social.application;

import com.studywithus.checkin.domain.Checkin;
import com.studywithus.checkin.domain.CheckinRepository;
import com.studywithus.community.domain.Community;
import com.studywithus.community.domain.CommunityMemberRepository;
import com.studywithus.community.domain.CommunityRepository;
import com.studywithus.shared.domain.DomainException;
import com.studywithus.social.domain.FeedTimelineEntry;
import com.studywithus.social.domain.FeedTimelinePort;
import com.studywithus.social.domain.FeedTimelineSlice;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class GetCommunityFeedUseCase {

    private final CommunityRepository communityRepository;
    private final CommunityMemberRepository communityMemberRepository;
    private final CheckinRepository checkinRepository;
    private final FeedItemHydrator feedItemHydrator;
    private final FeedTimelinePort feedTimelinePort;

    @Autowired
    public GetCommunityFeedUseCase(
            CommunityRepository communityRepository,
            CommunityMemberRepository communityMemberRepository,
            CheckinRepository checkinRepository,
            FeedItemHydrator feedItemHydrator,
            @Autowired(required = false) FeedTimelinePort feedTimelinePort
    ) {
        this.communityRepository = communityRepository;
        this.communityMemberRepository = communityMemberRepository;
        this.checkinRepository = checkinRepository;
        this.feedItemHydrator = feedItemHydrator;
        this.feedTimelinePort = feedTimelinePort;
    }

    @Transactional(readOnly = true)
    public FeedCursorResult execute(UUID communityId, UUID callerId, String cursor, int limit) {
        Community community = communityRepository.findById(communityId)
                .filter(Community::isActive)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "COMMUNITY_NOT_FOUND", "Community not found"));

        if (community.isPrivate()) {
            if (callerId == null || !communityMemberRepository.isUserActiveMember(communityId, callerId)) {
                throw new DomainException(
                        HttpStatus.FORBIDDEN,
                        "COMMUNITY_ACCESS_DENIED",
                        "Only active members can view the feed of a private community"
                );
            }
        }

        int boundedLimit = limit > 0 ? Math.min(limit, 50) : 20;
        FeedCursorHelper.FeedCursor decodedCursor = FeedCursorHelper.decodeCursor(cursor);
        Instant cursorInstant = decodedCursor != null ? decodedCursor.createdAt() : null;
        UUID cursorId = decodedCursor != null ? decodedCursor.id() : null;

        if (feedTimelinePort != null) {
            FeedTimelineSlice slice = feedTimelinePort.getCommunityTimeline(communityId, cursorInstant, cursorId, boundedLimit + 1);
            if (slice.cacheHit()) {
                List<Checkin> checkins = feedItemHydrator.hydrateInOrder(slice.checkinIds(), checkinRepository);
                return feedItemHydrator.toCursorResult(checkins, boundedLimit, callerId);
            }
        }

        List<Checkin> checkins = checkinRepository.findFeedCheckins(
                List.of(communityId),
                Collections.emptyList(),
                cursorInstant,
                cursorId,
                boundedLimit + 1
        );

        if (cursorInstant == null && feedTimelinePort != null && !checkins.isEmpty()) {
            List<FeedTimelineEntry> entries = checkins.stream()
                    .map(c -> new FeedTimelineEntry(c.getId(), c.getCreatedAt()))
                    .toList();
            feedTimelinePort.warmCommunityTimeline(communityId, entries);
        }

        return feedItemHydrator.toCursorResult(checkins, boundedLimit, callerId);
    }
}
