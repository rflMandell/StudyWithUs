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
import com.studywithus.social.domain.FollowRepository;
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
        when(checkinRepository.findFeedCheckins(eq(List.of(communityId)), eq(Collections.emptyList()), any(), eq(2)))
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
        assertThat(FeedCursorHelper.decode(result.nextCursor())).isEqualTo(c1.getCreatedAt());
    }
}
