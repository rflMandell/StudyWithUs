package com.studywithus.social.application;

import com.studywithus.identity.domain.User;
import com.studywithus.identity.domain.UserRepository;
import com.studywithus.identity.domain.UserStatus;
import com.studywithus.shared.domain.DomainException;
import com.studywithus.social.domain.FeedTimelinePort;
import com.studywithus.social.domain.Follow;
import com.studywithus.social.domain.FollowRepository;
import com.studywithus.social.domain.FollowRequest;
import com.studywithus.social.domain.FollowRequestRepository;
import com.studywithus.social.domain.FollowRequestStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FollowUseCasesTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private FollowRepository followRepository;
    @Mock
    private FollowRequestRepository followRequestRepository;
    @Mock
    private FeedTimelinePort feedTimelinePort;

    private FollowUserUseCase followUserUseCase;
    private UnfollowUserUseCase unfollowUserUseCase;
    private RespondFollowRequestUseCase respondFollowRequestUseCase;
    private GetFollowStatusUseCase getFollowStatusUseCase;

    private UUID aliceId;
    private UUID bobId;

    @BeforeEach
    void setUp() {
        followUserUseCase = new FollowUserUseCase(userRepository, followRepository, followRequestRepository, feedTimelinePort);
        unfollowUserUseCase = new UnfollowUserUseCase(followRepository, followRequestRepository, feedTimelinePort);
        respondFollowRequestUseCase = new RespondFollowRequestUseCase(followRequestRepository, followRepository, feedTimelinePort);
        getFollowStatusUseCase = new GetFollowStatusUseCase(userRepository, followRepository, followRequestRepository);
        aliceId = UUID.randomUUID();
        bobId = UUID.randomUUID();
    }

    @Test
    void followSelfThrowsBadRequest() {
        assertThatThrownBy(() -> followUserUseCase.execute(aliceId, aliceId))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Cannot follow yourself");
    }

    @Test
    void followPublicUserCreatesFollowImmediately() {
        User bob = User.builder().id(bobId).status(UserStatus.ACTIVE).isPrivate(false).build();
        when(userRepository.findById(bobId)).thenReturn(Optional.of(bob));
        when(followRepository.existsByFollowerIdAndFollowingId(aliceId, bobId)).thenReturn(false);

        FollowActionResult result = followUserUseCase.execute(aliceId, bobId);

        assertThat(result.status()).isEqualTo(FollowRelationshipStatus.FOLLOWING);
        verify(followRepository).save(any(Follow.class));
        verify(feedTimelinePort).invalidateUserTimeline(aliceId);
    }

    @Test
    void followPrivateUserCreatesPendingRequest() {
        User bob = User.builder().id(bobId).status(UserStatus.ACTIVE).isPrivate(true).build();
        when(userRepository.findById(bobId)).thenReturn(Optional.of(bob));
        when(followRepository.existsByFollowerIdAndFollowingId(aliceId, bobId)).thenReturn(false);
        when(followRequestRepository.findByRequesterIdAndTargetId(aliceId, bobId)).thenReturn(Optional.empty());

        FollowActionResult result = followUserUseCase.execute(aliceId, bobId);

        assertThat(result.status()).isEqualTo(FollowRelationshipStatus.REQUESTED);
        verify(followRequestRepository).save(any(FollowRequest.class));
        verify(followRepository, never()).save(any());
    }

    @Test
    void followPrivateUserReopensRejectedRequest() {
        User bob = User.builder().id(bobId).status(UserStatus.ACTIVE).isPrivate(true).build();
        FollowRequest rejected = FollowRequest.builder()
                .id(UUID.randomUUID())
                .requesterId(aliceId)
                .targetId(bobId)
                .status(FollowRequestStatus.REJECTED)
                .build();
        when(userRepository.findById(bobId)).thenReturn(Optional.of(bob));
        when(followRepository.existsByFollowerIdAndFollowingId(aliceId, bobId)).thenReturn(false);
        when(followRequestRepository.findByRequesterIdAndTargetId(aliceId, bobId)).thenReturn(Optional.of(rejected));

        FollowActionResult result = followUserUseCase.execute(aliceId, bobId);

        assertThat(result.status()).isEqualTo(FollowRelationshipStatus.REQUESTED);
        assertThat(rejected.getStatus()).isEqualTo(FollowRequestStatus.PENDING);
        verify(followRequestRepository).save(rejected);
    }

    @Test
    void acceptFollowRequestCreatesFollowAndInvalidatesRequesterTimeline() {
        UUID requestId = UUID.randomUUID();
        FollowRequest request = FollowRequest.create(aliceId, bobId);
        when(followRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(followRepository.existsByFollowerIdAndFollowingId(aliceId, bobId)).thenReturn(false);

        respondFollowRequestUseCase.execute(requestId, bobId, true);

        assertThat(request.getStatus()).isEqualTo(FollowRequestStatus.ACCEPTED);
        verify(followRequestRepository).save(request);
        verify(followRepository).save(any(Follow.class));
        verify(feedTimelinePort).invalidateUserTimeline(aliceId);
    }

    @Test
    void rejectFollowRequestDoesNotCreateFollow() {
        UUID requestId = UUID.randomUUID();
        FollowRequest request = FollowRequest.create(aliceId, bobId);
        when(followRequestRepository.findById(requestId)).thenReturn(Optional.of(request));

        respondFollowRequestUseCase.execute(requestId, bobId, false);

        assertThat(request.getStatus()).isEqualTo(FollowRequestStatus.REJECTED);
        verify(followRequestRepository).save(request);
        verify(followRepository, never()).save(any());
    }

    @Test
    void respondFollowRequestByNonTargetThrowsForbidden() {
        UUID requestId = UUID.randomUUID();
        FollowRequest request = FollowRequest.create(aliceId, bobId);
        when(followRequestRepository.findById(requestId)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> respondFollowRequestUseCase.execute(requestId, aliceId, true))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("permission");
    }

    @Test
    void unfollowRemovesFollowAndRequest() {
        when(followRepository.existsByFollowerIdAndFollowingId(aliceId, bobId)).thenReturn(true);

        FollowActionResult result = unfollowUserUseCase.execute(aliceId, bobId);

        assertThat(result.status()).isEqualTo(FollowRelationshipStatus.NONE);
        verify(followRepository).deleteByFollowerIdAndFollowingId(aliceId, bobId);
        verify(followRequestRepository).deleteByRequesterIdAndTargetId(aliceId, bobId);
        verify(feedTimelinePort).invalidateUserTimeline(aliceId);
    }

    @Test
    void getFollowStatusReturnsSelfAndCounts() {
        User alice = User.builder().id(aliceId).status(UserStatus.ACTIVE).build();
        when(userRepository.findById(aliceId)).thenReturn(Optional.of(alice));
        when(followRepository.countByFollowingId(aliceId)).thenReturn(5L);
        when(followRepository.countByFollowerId(aliceId)).thenReturn(3L);

        FollowStatusResult result = getFollowStatusUseCase.execute(aliceId, aliceId);

        assertThat(result.status()).isEqualTo(FollowRelationshipStatus.SELF);
        assertThat(result.followersCount()).isEqualTo(5L);
        assertThat(result.followingCount()).isEqualTo(3L);
    }
}
