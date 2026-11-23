package com.studywithus.social.application;

import com.studywithus.checkin.domain.Checkin;
import com.studywithus.checkin.domain.CheckinCreatedEvent;
import com.studywithus.checkin.domain.CheckinDeletedEvent;
import com.studywithus.checkin.domain.CheckinRepository;
import com.studywithus.community.domain.Community;
import com.studywithus.community.domain.CommunityMemberJoinedEvent;
import com.studywithus.community.domain.CommunityMemberRemovedEvent;
import com.studywithus.community.domain.CommunityMemberRepository;
import com.studywithus.community.domain.CommunityRepository;
import com.studywithus.community.domain.CommunityRole;
import com.studywithus.community.domain.CommunityVisibility;
import com.studywithus.moderation.domain.ModerationAction;
import com.studywithus.moderation.domain.ReportResolvedEvent;
import com.studywithus.moderation.domain.ReportStatus;
import com.studywithus.moderation.domain.ReportTargetType;
import com.studywithus.social.domain.FeedTimelinePort;
import com.studywithus.social.domain.FollowRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeedProjectionEventListenerTest {

    @Mock
    private FeedTimelinePort feedTimelinePort;

    @Mock
    private CommunityMemberRepository communityMemberRepository;

    @Mock
    private CommunityRepository communityRepository;

    @Mock
    private FollowRepository followRepository;

    @Mock
    private CheckinRepository checkinRepository;

    private SimpleMeterRegistry meterRegistry;
    private FeedProjectionEventListener listener;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        listener = new FeedProjectionEventListener(
                feedTimelinePort,
                communityMemberRepository,
                communityRepository,
                followRepository,
                checkinRepository,
                meterRegistry
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldFanOutCheckinTo500CommunityMembersAndFollowersForPublicCommunity() {
        UUID checkinId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID communityId = UUID.randomUUID();
        Instant now = Instant.now();

        List<UUID> members = new ArrayList<>(500);
        for (int i = 0; i < 500; i++) {
            members.add(UUID.randomUUID());
        }
        UUID extraFollower = UUID.randomUUID();

        when(communityMemberRepository.findActiveUserIdsByCommunityId(communityId)).thenReturn(members);
        when(communityRepository.findById(communityId)).thenReturn(Optional.of(Community.builder()
                .id(communityId)
                .name("Public Study Group")
                .visibility(CommunityVisibility.PUBLIC)
                .timezone("America/Sao_Paulo")
                .build()));
        when(followRepository.findFollowerIdsByFollowingId(authorId)).thenReturn(List.of(extraFollower));

        listener.onCheckinCreated(new CheckinCreatedEvent(checkinId, authorId, communityId, now));

        ArgumentCaptor<Collection<UUID>> recipientsCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(feedTimelinePort).addCheckinToTimelines(eq(checkinId), eq(now), eq(communityId), recipientsCaptor.capture());

        assertThat(recipientsCaptor.getValue()).hasSize(502); // 500 members + author + extraFollower
        assertThat(recipientsCaptor.getValue()).contains(authorId, extraFollower);
        assertThat(meterRegistry.counter("studywithus_feed_fanout_total").count()).isEqualTo(1.0);
        assertThat(meterRegistry.counter("studywithus_feed_fanout_recipients_total").count()).isEqualTo(502.0);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldExcludeNonMemberFollowersWhenCommunityIsPrivate() {
        UUID checkinId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        UUID communityId = UUID.randomUUID();
        Instant now = Instant.now();

        when(communityMemberRepository.findActiveUserIdsByCommunityId(communityId)).thenReturn(List.of(authorId, memberId));
        when(communityRepository.findById(communityId)).thenReturn(Optional.of(Community.builder()
                .id(communityId)
                .name("Private Study Group")
                .visibility(CommunityVisibility.PRIVATE)
                .timezone("America/Sao_Paulo")
                .build()));

        listener.onCheckinCreated(new CheckinCreatedEvent(checkinId, authorId, communityId, now));

        ArgumentCaptor<Collection<UUID>> recipientsCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(feedTimelinePort).addCheckinToTimelines(eq(checkinId), eq(now), eq(communityId), recipientsCaptor.capture());

        assertThat(recipientsCaptor.getValue()).containsExactlyInAnyOrder(authorId, memberId);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldRemoveCheckinFromTimelinesOnDeleteAndModerationRemoval() {
        UUID checkinId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        UUID communityId = UUID.randomUUID();

        when(communityMemberRepository.findActiveUserIdsByCommunityId(communityId)).thenReturn(List.of(authorId, memberId));
        when(communityRepository.findById(communityId)).thenReturn(Optional.of(Community.builder()
                .id(communityId)
                .name("Public Group")
                .visibility(CommunityVisibility.PUBLIC)
                .timezone("America/Sao_Paulo")
                .build()));
        when(followRepository.findFollowerIdsByFollowingId(authorId)).thenReturn(List.of());

        listener.onCheckinDeleted(new CheckinDeletedEvent(checkinId, authorId, communityId, authorId));

        ArgumentCaptor<Collection<UUID>> recipientsCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(feedTimelinePort).removeCheckinFromTimelines(eq(checkinId), eq(communityId), recipientsCaptor.capture());
        assertThat(recipientsCaptor.getValue()).containsExactlyInAnyOrder(authorId, memberId);

        UUID moderatedCheckinId = UUID.randomUUID();
        when(checkinRepository.findByIdIncludingDeleted(moderatedCheckinId)).thenReturn(Optional.of(
                Checkin.builder().id(moderatedCheckinId).userId(authorId).communityId(communityId).build()
        ));

        listener.onReportResolved(new ReportResolvedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                ReportStatus.RESOLVED,
                ModerationAction.REMOVE_CONTENT,
                ReportTargetType.CHECKIN,
                moderatedCheckinId
        ));

        verify(feedTimelinePort).removeCheckinFromTimelines(eq(moderatedCheckinId), eq(communityId), eq(recipientsCaptor.getValue()));
    }

    @Test
    void shouldInvalidateUserTimelineOnMembershipChange() {
        UUID userId = UUID.randomUUID();
        UUID communityId = UUID.randomUUID();

        listener.onCommunityMemberJoined(new CommunityMemberJoinedEvent(communityId, userId, CommunityRole.MEMBER, Instant.now()));
        listener.onCommunityMemberRemoved(new CommunityMemberRemovedEvent(communityId, userId, UUID.randomUUID()));

        verify(feedTimelinePort, org.mockito.Mockito.times(2)).invalidateUserTimeline(userId);
    }
}
