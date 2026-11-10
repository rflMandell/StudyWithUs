package com.studywithus.social.domain;

import com.studywithus.shared.domain.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CommentTest {

    @Test
    @DisplayName("should create comment successfully with trimmed content")
    void shouldCreateCommentSuccessfully() {
        UUID checkinId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();

        Comment comment = Comment.create(checkinId, authorId, "  Ótimo progresso nos estudos!  ");

        assertThat(comment.getId()).isNotNull();
        assertThat(comment.getCheckinId()).isEqualTo(checkinId);
        assertThat(comment.getAuthorId()).isEqualTo(authorId);
        assertThat(comment.getContent()).isEqualTo("Ótimo progresso nos estudos!");
        assertThat(comment.getCreatedAt()).isNotNull();
        assertThat(comment.getUpdatedAt()).isNotNull();
        assertThat(comment.getDeletedAt()).isNull();
        assertThat(comment.isActive()).isTrue();
    }

    @Test
    @DisplayName("should throw when comment is empty or blank")
    void shouldThrowWhenContentIsBlank() {
        UUID checkinId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();

        assertThatThrownBy(() -> Comment.create(checkinId, authorId, "   "))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> {
                    DomainException de = (DomainException) ex;
                    assertThat(de.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(de.getCode()).isEqualTo("COMMENT_EMPTY");
                });
    }

    @Test
    @DisplayName("should throw when comment exceeds 1000 characters")
    void shouldThrowWhenContentExceedsMax() {
        UUID checkinId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        String longContent = "a".repeat(1001);

        assertThatThrownBy(() -> Comment.create(checkinId, authorId, longContent))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> {
                    DomainException de = (DomainException) ex;
                    assertThat(de.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(de.getCode()).isEqualTo("COMMENT_TOO_LONG");
                });
    }

    @Test
    @DisplayName("should soft-delete when author deletes comment")
    void shouldSoftDeleteWhenAuthorDeletes() {
        UUID checkinId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        Comment comment = Comment.create(checkinId, authorId, "Parabéns!");

        comment.delete(authorId);

        assertThat(comment.getDeletedAt()).isNotNull();
        assertThat(comment.isActive()).isFalse();
    }

    @Test
    @DisplayName("should throw 403 when non-author attempts deletion")
    void shouldThrowWhenNonAuthorAttemptsDeletion() {
        UUID checkinId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        Comment comment = Comment.create(checkinId, authorId, "Parabéns!");

        assertThatThrownBy(() -> comment.delete(otherUserId))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> {
                    DomainException de = (DomainException) ex;
                    assertThat(de.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(de.getCode()).isEqualTo("COMMENT_FORBIDDEN");
                });
    }
}
