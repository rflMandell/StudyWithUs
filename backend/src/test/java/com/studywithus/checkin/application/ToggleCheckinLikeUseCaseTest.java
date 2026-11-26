package com.studywithus.checkin.application;

import com.studywithus.checkin.domain.Checkin;
import com.studywithus.checkin.domain.CheckinLikeRepository;
import com.studywithus.checkin.domain.CheckinLikedEvent;
import com.studywithus.checkin.domain.CheckinRepository;
import com.studywithus.community.domain.Community;
import com.studywithus.community.domain.CommunityMemberRepository;
import com.studywithus.community.domain.CommunityRepository;
import com.studywithus.community.domain.CommunityVisibility;
import com.studywithus.shared.domain.DomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ToggleCheckinLikeUseCaseTest {

    @Mock
    private CheckinRepository checkinRepository;
    @Mock
    private CheckinLikeRepository checkinLikeRepository;
    @Mock
    private CommunityRepository communityRepository;
    @Mock
    private CommunityMemberRepository communityMemberRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private ToggleCheckinLikeUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ToggleCheckinLikeUseCase(
                checkinRepository,
                checkinLikeRepository,
                communityRepository,
                communityMemberRepository,
                eventPublisher
        );
    }

    @Test
    @DisplayName("should like checkin and publish CheckinLikedEvent when not yet liked")
    void shouldLikeCheckin() {
        UUID checkinId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID likerId = UUID.randomUUID();
        UUID communityId = UUID.randomUUID();

        Checkin checkin = Checkin.builder()
                .id(checkinId)
                .userId(authorId)
                .communityId(communityId)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        Community community = Community.builder()
                .id(communityId)
                .visibility(CommunityVisibility.PUBLIC)
                .build();

        when(checkinRepository.findById(checkinId)).thenReturn(Optional.of(checkin));
        when(communityRepository.findById(communityId)).thenReturn(Optional.of(community));
        when(checkinLikeRepository.exists(checkinId, likerId)).thenReturn(false);
        when(checkinLikeRepository.countByCheckinId(checkinId)).thenReturn(1L);

        LikeToggleResult result = useCase.execute(new ToggleCheckinLikeCommand(checkinId, likerId));

        assertThat(result.targetId()).isEqualTo(checkinId);
        assertThat(result.liked()).isTrue();
        assertThat(result.likesCount()).isEqualTo(1L);
        verify(checkinLikeRepository).save(checkinId, likerId);

        ArgumentCaptor<CheckinLikedEvent> captor = ArgumentCaptor.forClass(CheckinLikedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().checkinId()).isEqualTo(checkinId);
        assertThat(captor.getValue().checkinAuthorId()).isEqualTo(authorId);
        assertThat(captor.getValue().likerUserId()).isEqualTo(likerId);
    }

    @Test
    @DisplayName("should unlike checkin and not publish event when already liked")
    void shouldUnlikeCheckin() {
        UUID checkinId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID likerId = UUID.randomUUID();
        UUID communityId = UUID.randomUUID();

        Checkin checkin = Checkin.builder()
                .id(checkinId)
                .userId(authorId)
                .communityId(communityId)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        Community community = Community.builder()
                .id(communityId)
                .visibility(CommunityVisibility.PUBLIC)
                .build();

        when(checkinRepository.findById(checkinId)).thenReturn(Optional.of(checkin));
        when(communityRepository.findById(communityId)).thenReturn(Optional.of(community));
        when(checkinLikeRepository.exists(checkinId, likerId)).thenReturn(true);
        when(checkinLikeRepository.countByCheckinId(checkinId)).thenReturn(0L);

        LikeToggleResult result = useCase.execute(new ToggleCheckinLikeCommand(checkinId, likerId));

        assertThat(result.liked()).isFalse();
        assertThat(result.likesCount()).isZero();
        verify(checkinLikeRepository).delete(checkinId, likerId);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("should throw 403 when non-member tries to like private community checkin")
    void shouldThrow403ForPrivateCommunityNonMember() {
        UUID checkinId = UUID.randomUUID();
        UUID likerId = UUID.randomUUID();
        UUID communityId = UUID.randomUUID();

        Checkin checkin = Checkin.builder()
                .id(checkinId)
                .userId(UUID.randomUUID())
                .communityId(communityId)
                .build();
        Community community = Community.builder()
                .id(communityId)
                .visibility(CommunityVisibility.PRIVATE)
                .build();

        when(checkinRepository.findById(checkinId)).thenReturn(Optional.of(checkin));
        when(communityRepository.findById(communityId)).thenReturn(Optional.of(community));
        when(communityMemberRepository.isUserActiveMember(communityId, likerId)).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(new ToggleCheckinLikeCommand(checkinId, likerId)))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }
}
