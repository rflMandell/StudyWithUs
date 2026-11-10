package com.studywithus.social.application;

import com.studywithus.checkin.domain.Checkin;
import com.studywithus.checkin.domain.CheckinRepository;
import com.studywithus.community.domain.CommunityMemberRepository;
import com.studywithus.identity.domain.User;
import com.studywithus.identity.domain.UserRepository;
import com.studywithus.identity.domain.UserStatus;
import com.studywithus.shared.domain.DomainException;
import com.studywithus.social.domain.Comment;
import com.studywithus.social.domain.CommentCreatedEvent;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateCommentUseCaseTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private CheckinRepository checkinRepository;

    @Mock
    private CommunityMemberRepository communityMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private CreateCommentUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CreateCommentUseCase(
                commentRepository,
                checkinRepository,
                communityMemberRepository,
                userRepository,
                eventPublisher
        );
    }

    @Test
    @DisplayName("should create comment and publish event when user is active member")
    void shouldCreateCommentSuccessfully() {
        UUID checkinId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID communityId = UUID.randomUUID();

        Checkin checkin = Checkin.builder()
                .id(checkinId)
                .communityId(communityId)
                .userId(UUID.randomUUID())
                .caption("Estudando Java")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        User author = User.builder()
                .id(authorId)
                .username("joaosilva")
                .name("João Silva")
                .avatarUrl("https://avatar.com/joao.png")
                .status(UserStatus.ACTIVE)
                .build();

        when(checkinRepository.findById(checkinId)).thenReturn(Optional.of(checkin));
        when(communityMemberRepository.isUserActiveMember(communityId, authorId)).thenReturn(true);
        when(userRepository.findById(authorId)).thenReturn(Optional.of(author));
        when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> inv.getArgument(0));

        CommentResult result = useCase.execute(new CreateCommentCommand(checkinId, authorId, "Excelente!"));

        assertThat(result.id()).isNotNull();
        assertThat(result.checkinId()).isEqualTo(checkinId);
        assertThat(result.authorId()).isEqualTo(authorId);
        assertThat(result.authorUsername()).isEqualTo("joaosilva");
        assertThat(result.authorName()).isEqualTo("João Silva");
        assertThat(result.content()).isEqualTo("Excelente!");

        ArgumentCaptor<CommentCreatedEvent> eventCaptor = ArgumentCaptor.forClass(CommentCreatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().commentId()).isEqualTo(result.id());
        assertThat(eventCaptor.getValue().communityId()).isEqualTo(communityId);
    }

    @Test
    @DisplayName("should throw 403 when user is not active member of the checkin community")
    void shouldThrow403WhenUserIsNotCommunityMember() {
        UUID checkinId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID communityId = UUID.randomUUID();

        Checkin checkin = Checkin.builder()
                .id(checkinId)
                .communityId(communityId)
                .userId(UUID.randomUUID())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(checkinRepository.findById(checkinId)).thenReturn(Optional.of(checkin));
        when(communityMemberRepository.isUserActiveMember(communityId, authorId)).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(new CreateCommentCommand(checkinId, authorId, "Comentário")))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> {
                    DomainException de = (DomainException) ex;
                    assertThat(de.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(de.getCode()).isEqualTo("COMMUNITY_NOT_A_MEMBER");
                });
    }

    @Test
    @DisplayName("should throw 404 when checkin does not exist")
    void shouldThrow404WhenCheckinNotFound() {
        UUID checkinId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();

        when(checkinRepository.findById(checkinId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(new CreateCommentCommand(checkinId, authorId, "Comentário")))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> {
                    DomainException de = (DomainException) ex;
                    assertThat(de.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(de.getCode()).isEqualTo("CHECKIN_NOT_FOUND");
                });
    }
}
