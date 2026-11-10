package com.studywithus.social.application;

import com.studywithus.checkin.domain.Checkin;
import com.studywithus.checkin.domain.CheckinRepository;
import com.studywithus.shared.domain.DomainException;
import com.studywithus.social.domain.Comment;
import com.studywithus.social.domain.CommentDeletedEvent;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteCommentUseCaseTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private CheckinRepository checkinRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private DeleteCommentUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new DeleteCommentUseCase(commentRepository, checkinRepository, eventPublisher);
    }

    @Test
    @DisplayName("should delete comment when user is author and publish event")
    void shouldDeleteCommentSuccessfully() {
        UUID commentId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID checkinId = UUID.randomUUID();
        UUID communityId = UUID.randomUUID();

        Comment comment = Comment.builder()
                .id(commentId)
                .checkinId(checkinId)
                .authorId(authorId)
                .content("Comentário")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Checkin checkin = Checkin.builder()
                .id(checkinId)
                .communityId(communityId)
                .build();

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        when(checkinRepository.findById(checkinId)).thenReturn(Optional.of(checkin));

        useCase.execute(new DeleteCommentCommand(commentId, authorId));

        assertThat(comment.getDeletedAt()).isNotNull();
        verify(commentRepository).save(comment);

        ArgumentCaptor<CommentDeletedEvent> eventCaptor = ArgumentCaptor.forClass(CommentDeletedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().commentId()).isEqualTo(commentId);
        assertThat(eventCaptor.getValue().communityId()).isEqualTo(communityId);
    }

    @Test
    @DisplayName("should throw 403 when user is not comment author")
    void shouldThrow403WhenNotAuthor() {
        UUID commentId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();

        Comment comment = Comment.builder()
                .id(commentId)
                .checkinId(UUID.randomUUID())
                .authorId(authorId)
                .content("Comentário")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));

        assertThatThrownBy(() -> useCase.execute(new DeleteCommentCommand(commentId, otherUserId)))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> {
                    DomainException de = (DomainException) ex;
                    assertThat(de.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(de.getCode()).isEqualTo("COMMENT_FORBIDDEN");
                });
    }

    @Test
    @DisplayName("should throw 404 when comment does not exist")
    void shouldThrow404WhenCommentNotFound() {
        UUID commentId = UUID.randomUUID();
        when(commentRepository.findById(commentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(new DeleteCommentCommand(commentId, UUID.randomUUID())))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> {
                    DomainException de = (DomainException) ex;
                    assertThat(de.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(de.getCode()).isEqualTo("COMMENT_NOT_FOUND");
                });
    }
}
