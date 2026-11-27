package com.studywithus.social.application;

import com.studywithus.checkin.domain.Checkin;
import com.studywithus.checkin.domain.CheckinRepository;
import com.studywithus.community.domain.Community;
import com.studywithus.community.domain.CommunityMemberRepository;
import com.studywithus.community.domain.CommunityRepository;
import com.studywithus.identity.domain.UserRepository;
import com.studywithus.social.domain.CommentRepository;
import com.studywithus.social.domain.FeedTimelineEntry;
import com.studywithus.social.domain.FeedTimelinePort;
import com.studywithus.social.domain.FeedTimelineSlice;
import com.studywithus.social.domain.FollowRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class GetUserFeedUseCase {

    private final CommunityMemberRepository communityMemberRepository;
    private final FollowRepository followRepository;
    private final CheckinRepository checkinRepository;
    private final FeedItemHydrator feedItemHydrator;
    private final FeedTimelinePort feedTimelinePort;
    private final Counter cacheHitCounter;
    private final Counter cacheMissCounter;

    @Autowired
    public GetUserFeedUseCase(
            CommunityMemberRepository communityMemberRepository,
            FollowRepository followRepository,
            CheckinRepository checkinRepository,
            FeedItemHydrator feedItemHydrator,
            @Autowired(required = false) FeedTimelinePort feedTimelinePort,
            @Autowired(required = false) MeterRegistry meterRegistry
    ) {
        this.communityMemberRepository = communityMemberRepository;
        this.followRepository = followRepository;
        this.checkinRepository = checkinRepository;
        this.feedItemHydrator = feedItemHydrator;
        this.feedTimelinePort = feedTimelinePort;
        this.cacheHitCounter = meterRegistry != null
                ? Counter.builder("studywithus_feed_cache_hits_total")
                        .description("Total number of feed queries served from Redis materialized timeline")
                        .register(meterRegistry)
                : null;
        this.cacheMissCounter = meterRegistry != null
                ? Counter.builder("studywithus_feed_cache_misses_total")
                        .description("Total number of feed queries served from PostgreSQL")
                        .register(meterRegistry)
                : null;
    }

    public GetUserFeedUseCase(
            CommunityMemberRepository communityMemberRepository,
            FollowRepository followRepository,
            CheckinRepository checkinRepository,
            UserRepository userRepository,
            CommunityRepository communityRepository,
            CommentRepository commentRepository
    ) {
        this(
                communityMemberRepository,
                followRepository,
                checkinRepository,
                new FeedItemHydrator(userRepository, communityRepository, commentRepository),
                null,
                null
        );
    }

    @Transactional(readOnly = true)
    public FeedCursorResult execute(GetUserFeedQuery query) {
        int limit = query.limit() > 0 ? Math.min(query.limit(), 50) : 20;
        FeedCursorHelper.FeedCursor decodedCursor = FeedCursorHelper.decodeCursor(query.cursor());
        Instant cursorInstant = decodedCursor != null ? decodedCursor.createdAt() : null;
        UUID cursorId = decodedCursor != null ? decodedCursor.id() : null;

        if (feedTimelinePort != null) {
            FeedTimelineSlice slice = feedTimelinePort.getUserTimeline(query.userId(), cursorInstant, cursorId, limit + 1);
            if (slice.cacheHit()) {
                if (cacheHitCounter != null) {
                    cacheHitCounter.increment();
                }
                List<Checkin> checkins = feedItemHydrator.hydrateInOrder(slice.checkinIds(), checkinRepository);
                return feedItemHydrator.toCursorResult(checkins, limit, query.userId());
            }
        }

        if (cacheMissCounter != null) {
            cacheMissCounter.increment();
        }

        List<UUID> activeCommunityIds = communityMemberRepository.findActiveCommunityIdsByUserId(query.userId());
        List<UUID> followingIds = followRepository.findFollowingIdsByFollowerId(query.userId());

        List<Checkin> checkins = checkinRepository.findFeedCheckins(
                query.userId(),
                activeCommunityIds,
                followingIds,
                cursorInstant,
                cursorId,
                limit + 1
        );

        if (cursorInstant == null && feedTimelinePort != null && !checkins.isEmpty()) {
            List<FeedTimelineEntry> entries = checkins.stream()
                    .map(c -> new FeedTimelineEntry(c.getId(), c.getCreatedAt()))
                    .toList();
            feedTimelinePort.warmUserTimeline(query.userId(), entries);
        }

        return feedItemHydrator.toCursorResult(checkins, limit, query.userId());
    }
}
