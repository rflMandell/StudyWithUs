package com.studywithus.social.application;

import com.studywithus.checkin.domain.Checkin;
import com.studywithus.checkin.domain.CheckinRepository;
import com.studywithus.community.domain.Community;
import com.studywithus.community.domain.CommunityMemberRepository;
import com.studywithus.community.domain.CommunityRepository;
import com.studywithus.community.domain.CommunityVisibility;
import com.studywithus.identity.domain.User;
import com.studywithus.identity.domain.UserRepository;
import com.studywithus.identity.domain.UserStatus;
import com.studywithus.social.domain.CommentRepository;
import com.studywithus.social.domain.FeedTimelinePort;
import com.studywithus.social.domain.FeedTimelineSlice;
import com.studywithus.social.domain.FollowRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetUserFeedUseCaseTest {

    @Mock
    private CommunityMemberRepository communityMemberRepository;

    @Mock
    private FollowRepository followRepository;

    @Mock
    private CheckinRepository checkinRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CommunityRepository communityRepository;

    @Mock
    private CommentRepository commentRepository;

    private GetUserFeedUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetUserFeedUseCase(
                communityMemberRepository,
                followRepository,
                checkinRepository,
                userRepository,
                communityRepository,
                commentRepository
        );
    }

    @Test
    void shouldReturnEmptyResultWhenUserHasNoCommunitiesAndFollowsNobody() {
        UUID userId = UUID.randomUUID();
        when(communityMemberRepository.findActiveCommunityIdsByUserId(userId)).thenReturn(Collections.emptyList());
        when(followRepository.findFollowingIdsByFollowerId(userId)).thenReturn(Collections.emptyList());

        FeedCursorResult result = useCase.execute(new GetUserFeedQuery(userId, null, 20));

        assertThat(result.items()).isEmpty();
        assertThat(result.hasMore()).isFalse();
        assertThat(result.nextCursor()).isNull();
    }

    @Test
    void shouldCalculateCursorAndPaginationCorrectlyWhenItemsExceedLimit() {
        UUID userId = UUID.randomUUID();
        UUID communityId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();

        when(communityMemberRepository.findActiveCommunityIdsByUserId(userId)).thenReturn(List.of(communityId));
        when(followRepository.findFollowingIdsByFollowerId(userId)).thenReturn(Collections.emptyList());

        Instant now = Instant.now();
        Checkin c1 = Checkin.builder()
                .id(UUID.randomUUID())
                .userId(authorId)
                .communityId(communityId)
                .caption("Checkin 1")
                .createdAt(now.minusSeconds(10))
                .updatedAt(now.minusSeconds(10))
                .build();

        Checkin c2 = Checkin.builder()
                .id(UUID.randomUUID())
                .userId(authorId)
                .communityId(communityId)
                .caption("Checkin 2")
                .createdAt(now.minusSeconds(20))
                .updatedAt(now.minusSeconds(20))
                .build();

        // Limit is 1, so repository is requested with limit + 1 = 2 items
        when(checkinRepository.findFeedCheckins(eq(List.of(communityId)), eq(Collections.emptyList()), any(), any(), eq(2)))
                .thenReturn(List.of(c1, c2));

        User author = User.builder()
                .id(authorId)
                .username("author")
                .name("Author User")
                .status(UserStatus.ACTIVE)
                .build();

        Community community = Community.builder()
                .id(communityId)
                .name("Test Community")
                .visibility(CommunityVisibility.PUBLIC)
                .timezone("America/Sao_Paulo")
                .build();

        when(userRepository.findAllById(any())).thenReturn(List.of(author));
        when(communityRepository.findAllById(any())).thenReturn(List.of(community));

        FeedCursorResult result = useCase.execute(new GetUserFeedQuery(userId, null, 1));

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).id()).isEqualTo(c1.getId());
        assertThat(result.items().get(0).authorUsername()).isEqualTo("author");
        assertThat(result.items().get(0).communityName()).isEqualTo("Test Community");
        assertThat(result.hasMore()).isTrue();
        assertThat(result.nextCursor()).isNotNull();
        FeedCursorHelper.FeedCursor decodedCursor = FeedCursorHelper.decodeCursor(result.nextCursor());
        assertThat(decodedCursor.createdAt()).isEqualTo(c1.getCreatedAt());
        assertThat(decodedCursor.id()).isEqualTo(c1.getId());
    }

    @Test
    void shouldReturnNullNextCursorWhenHasMoreIsFalse() {
        UUID userId = UUID.randomUUID();
        UUID communityId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();

        when(communityMemberRepository.findActiveCommunityIdsByUserId(userId)).thenReturn(List.of(communityId));
        when(followRepository.findFollowingIdsByFollowerId(userId)).thenReturn(Collections.emptyList());

        Instant now = Instant.now();
        Checkin c1 = Checkin.builder()
                .id(UUID.randomUUID())
                .userId(authorId)
                .communityId(communityId)
                .caption("Only Checkin")
                .createdAt(now.minusSeconds(10))
                .updatedAt(now.minusSeconds(10))
                .build();

        when(checkinRepository.findFeedCheckins(eq(List.of(communityId)), eq(Collections.emptyList()), any(), any(), eq(6)))
                .thenReturn(List.of(c1));

        FeedCursorResult result = useCase.execute(new GetUserFeedQuery(userId, null, 5));

        assertThat(result.items()).hasSize(1);
        assertThat(result.hasMore()).isFalse();
        assertThat(result.nextCursor()).isNull();
    }

    @Test
    void shouldServeFeedFromRedisTimelineOnCacheHitWithoutQueryingFeedSql() {
        FeedTimelinePort timelinePort = org.mockito.Mockito.mock(FeedTimelinePort.class);
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        GetUserFeedUseCase cachedUseCase = new GetUserFeedUseCase(
                communityMemberRepository,
                followRepository,
                checkinRepository,
                new FeedItemHydrator(userRepository, communityRepository, commentRepository),
                timelinePort,
                meterRegistry
        );

        UUID userId = UUID.randomUUID();
        UUID communityId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID checkinId = UUID.randomUUID();
        Instant now = Instant.now();

        when(timelinePort.getUserTimeline(userId, null, null, 21))
                .thenReturn(FeedTimelineSlice.hit(List.of(checkinId)));

        Checkin checkin = Checkin.builder()
                .id(checkinId)
                .userId(authorId)
                .communityId(communityId)
                .caption("Cached checkin")
                .createdAt(now)
                .updatedAt(now)
                .build();
        when(checkinRepository.findAllActiveByIds(List.of(checkinId))).thenReturn(List.of(checkin));

        FeedCursorResult result = cachedUseCase.execute(new GetUserFeedQuery(userId, null, 20));

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).id()).isEqualTo(checkinId);
        assertThat(meterRegistry.counter("studywithus_feed_cache_hits_total").count()).isEqualTo(1.0);
        org.mockito.Mockito.verifyNoInteractions(communityMemberRepository, followRepository);
    }

    @Test
    void shouldFallbackToPostgresAndWarmRedisOnCacheMiss() {
        FeedTimelinePort timelinePort = org.mockito.Mockito.mock(FeedTimelinePort.class);
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        GetUserFeedUseCase cachedUseCase = new GetUserFeedUseCase(
                communityMemberRepository,
                followRepository,
                checkinRepository,
                new FeedItemHydrator(userRepository, communityRepository, commentRepository),
                timelinePort,
                meterRegistry
        );

        UUID userId = UUID.randomUUID();
        UUID communityId = UUID.randomUUID();
        UUID checkinId = UUID.randomUUID();
        Instant now = Instant.now();

        when(timelinePort.getUserTimeline(userId, null, null, 21)).thenReturn(FeedTimelineSlice.miss());
        when(communityMemberRepository.findActiveCommunityIdsByUserId(userId)).thenReturn(List.of(communityId));
        when(followRepository.findFollowingIdsByFollowerId(userId)).thenReturn(Collections.emptyList());

        Checkin checkin = Checkin.builder()
                .id(checkinId)
                .userId(UUID.randomUUID())
                .communityId(communityId)
                .caption("Fallback checkin")
                .createdAt(now)
                .updatedAt(now)
                .build();
        when(checkinRepository.findFeedCheckins(eq(List.of(communityId)), eq(Collections.emptyList()), any(), any(), eq(21)))
                .thenReturn(List.of(checkin));

        FeedCursorResult result = cachedUseCase.execute(new GetUserFeedQuery(userId, null, 20));

        assertThat(result.items()).hasSize(1);
        assertThat(meterRegistry.counter("studywithus_feed_cache_misses_total").count()).isEqualTo(1.0);
        org.mockito.Mockito.verify(timelinePort).warmUserTimeline(eq(userId), any());
    }
}
