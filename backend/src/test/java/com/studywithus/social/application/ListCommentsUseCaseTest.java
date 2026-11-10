package com.studywithus.social.application;

import com.studywithus.checkin.domain.Checkin;
import com.studywithus.checkin.domain.CheckinRepository;
import com.studywithus.identity.domain.User;
import com.studywithus.identity.domain.UserRepository;
import com.studywithus.identity.domain.UserStatus;
import com.studywithus.shared.domain.DomainException;
import com.studywithus.shared.domain.PagedResult;
import com.studywithus.social.domain.Comment;
import com.studywithus.social.domain.CommentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListCommentsUseCaseTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private CheckinRepository checkinRepository;

    @Mock
    private UserRepository userRepository;

    private ListCommentsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ListCommentsUseCase(commentRepository, checkinRepository, userRepository);
    }

    @Test
    @DisplayName("should list comments enriched with author profile data")
    void shouldListCommentsEnriched() {
        UUID checkinId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();

        Checkin checkin = Checkin.builder()
                .id(checkinId)
                .communityId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Comment c1 = Comment.builder()
                .id(UUID.randomUUID())
                .checkinId(checkinId)
                .authorId(authorId)
                .content("Belo estudo!")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        User author = User.builder()
                .id(authorId)
                .username("maria")
                .name("Maria Silva")
                .avatarUrl("https://avatar.com/maria.png")
                .status(UserStatus.ACTIVE)
                .build();

        when(checkinRepository.findById(checkinId)).thenReturn(Optional.of(checkin));
        when(commentRepository.findByCheckinId(checkinId, 0, 20))
                .thenReturn(new PagedResult<>(List.of(c1), 0, 20, 1, 1, false));
        when(userRepository.findAllById(any())).thenReturn(List.of(author));

        PagedResult<CommentResult> result = useCase.execute(new ListCommentsQuery(checkinId, 0, 20));

        assertThat(result.content()).hasSize(1);
        CommentResult item = result.content().get(0);
        assertThat(item.id()).isEqualTo(c1.getId());
        assertThat(item.content()).isEqualTo("Belo estudo!");
        assertThat(item.authorUsername()).isEqualTo("maria");
        assertThat(item.authorName()).isEqualTo("Maria Silva");
        assertThat(item.authorAvatarUrl()).isEqualTo("https://avatar.com/maria.png");
    }

    @Test
    @DisplayName("should throw 404 when checkin does not exist")
    void shouldThrow404WhenCheckinNotFound() {
        UUID checkinId = UUID.randomUUID();
        when(checkinRepository.findById(checkinId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(new ListCommentsQuery(checkinId, 0, 20)))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> {
                    DomainException de = (DomainException) ex;
                    assertThat(de.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(de.getCode()).isEqualTo("CHECKIN_NOT_FOUND");
                });
    }
}
