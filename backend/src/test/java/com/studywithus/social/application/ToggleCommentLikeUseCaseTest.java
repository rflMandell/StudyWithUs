package com.studywithus.social.application;

import com.studywithus.checkin.application.LikeToggleResult;
import com.studywithus.checkin.domain.Checkin;
import com.studywithus.checkin.domain.CheckinRepository;
import com.studywithus.community.domain.Community;
import com.studywithus.community.domain.CommunityMemberRepository;
import com.studywithus.community.domain.CommunityRepository;
import com.studywithus.community.domain.CommunityVisibility;
import com.studywithus.shared.domain.DomainException;
import com.studywithus.social.domain.Comment;
import com.studywithus.social.domain.CommentLikeRepository;
import com.studywithus.social.domain.CommentLikedEvent;
import com.studywithus.social.domain.CommentRepository;
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
class ToggleCommentLikeUseCaseTest {

    @Mock
    private CommentRepository commentRepository;
    @Mock
    private CommentLikeRepository commentLikeRepository;
    @Mock
    private CheckinRepository checkinRepository;
    @Mock
    private CommunityRepository communityRepository;
    @Mock
    private CommunityMemberRepository communityMemberRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private ToggleCommentLikeUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ToggleCommentLikeUseCase(
                commentRepository,
                commentLikeRepository,
                checkinRepository,
                communityRepository,
                communityMemberRepository,
                eventPublisher
        );
    }

    @Test
    @DisplayName("should like comment and publish CommentLikedEvent when not yet liked")
    void shouldLikeComment() {
        UUID commentId = UUID.randomUUID();
        UUID checkinId = UUID.randomUUID();
        UUID commentAuthorId = UUID.randomUUID();
        UUID likerId = UUID.randomUUID();
        UUID communityId = UUID.randomUUID();

        Comment comment = Comment.builder()
                .id(commentId)
                .checkinId(checkinId)
                .authorId(commentAuthorId)
                .content("Ótimo!")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        Checkin checkin = Checkin.builder()
                .id(checkinId)
                .communityId(communityId)
                .userId(UUID.randomUUID())
                .build();
        Community community = Community.builder()
                .id(communityId)
                .visibility(CommunityVisibility.PUBLIC)
                .build();

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        when(checkinRepository.findById(checkinId)).thenReturn(Optional.of(checkin));
        when(communityRepository.findById(communityId)).thenReturn(Optional.of(community));
        when(commentLikeRepository.exists(commentId, likerId)).thenReturn(false);
        when(commentLikeRepository.countByCommentId(commentId)).thenReturn(1L);

        LikeToggleResult result = useCase.execute(new ToggleCommentLikeCommand(commentId, likerId));

        assertThat(result.targetId()).isEqualTo(commentId);
        assertThat(result.liked()).isTrue();
        assertThat(result.likesCount()).isEqualTo(1L);
        verify(commentLikeRepository).save(commentId, likerId);

        ArgumentCaptor<CommentLikedEvent> captor = ArgumentCaptor.forClass(CommentLikedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().commentId()).isEqualTo(commentId);
        assertThat(captor.getValue().commentAuthorId()).isEqualTo(commentAuthorId);
        assertThat(captor.getValue().likerUserId()).isEqualTo(likerId);
    }

    @Test
    @DisplayName("should unlike comment and not publish event when already liked")
    void shouldUnlikeComment() {
        UUID commentId = UUID.randomUUID();
        UUID checkinId = UUID.randomUUID();
        UUID likerId = UUID.randomUUID();
        UUID communityId = UUID.randomUUID();

        Comment comment = Comment.builder()
                .id(commentId)
                .checkinId(checkinId)
                .authorId(UUID.randomUUID())
                .content("Ótimo!")
                .build();
        Checkin checkin = Checkin.builder()
                .id(checkinId)
                .communityId(communityId)
                .userId(UUID.randomUUID())
                .build();
        Community community = Community.builder()
                .id(communityId)
                .visibility(CommunityVisibility.PUBLIC)
                .build();

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        when(checkinRepository.findById(checkinId)).thenReturn(Optional.of(checkin));
        when(communityRepository.findById(communityId)).thenReturn(Optional.of(community));
        when(commentLikeRepository.exists(commentId, likerId)).thenReturn(true);
        when(commentLikeRepository.countByCommentId(commentId)).thenReturn(0L);

        LikeToggleResult result = useCase.execute(new ToggleCommentLikeCommand(commentId, likerId));

        assertThat(result.liked()).isFalse();
        assertThat(result.likesCount()).isZero();
        verify(commentLikeRepository).delete(commentId, likerId);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("should throw 404 when comment does not exist")
    void shouldThrow404WhenCommentNotFound() {
        UUID commentId = UUID.randomUUID();
        when(commentRepository.findById(commentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(new ToggleCommentLikeCommand(commentId, UUID.randomUUID())))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }
}
