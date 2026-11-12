package com.studywithus.notification.application;

import com.studywithus.checkin.domain.Checkin;
import com.studywithus.checkin.domain.CheckinRepository;
import com.studywithus.community.domain.CommunityJoinRequestReviewedEvent;
import com.studywithus.community.domain.CommunityJoinRequestStatus;
import com.studywithus.gamification.domain.BadgeGrantedEvent;
import com.studywithus.gamification.domain.ChallengeCompletedEvent;
import com.studywithus.identity.domain.User;
import com.studywithus.identity.domain.UserRepository;
import com.studywithus.notification.domain.NotificationType;
import com.studywithus.social.domain.CommentCreatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

    @Mock
    private CreateInAppNotificationUseCase createInAppNotificationUseCase;

    @Mock
    private CheckinRepository checkinRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationEventListener listener;

    private UUID checkinAuthorId;
    private UUID commentAuthorId;
    private UUID checkinId;

    @BeforeEach
    void setUp() {
        checkinAuthorId = UUID.randomUUID();
        commentAuthorId = UUID.randomUUID();
        checkinId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Creates notification for check-in author when another user comments")
    void shouldCreateNotificationOnCommentCreated() {
        Checkin checkin = Checkin.builder()
                .id(checkinId)
                .userId(checkinAuthorId)
                .build();

        User commentAuthor = User.builder()
                .id(commentAuthorId)
                .username("joaosilva")
                .build();

        when(checkinRepository.findById(checkinId)).thenReturn(Optional.of(checkin));
        when(userRepository.findById(commentAuthorId)).thenReturn(Optional.of(commentAuthor));

        CommentCreatedEvent event = new CommentCreatedEvent(
                UUID.randomUUID(),
                checkinId,
                commentAuthorId,
                UUID.randomUUID()
        );

        listener.onCommentCreated(event);

        ArgumentCaptor<CreateNotificationCommand> captor = ArgumentCaptor.forClass(CreateNotificationCommand.class);
        verify(createInAppNotificationUseCase).execute(captor.capture());

        CreateNotificationCommand command = captor.getValue();
        assertThat(command.userId()).isEqualTo(checkinAuthorId);
        assertThat(command.type()).isEqualTo(NotificationType.NEW_COMMENT);
        assertThat(command.body()).contains("joaosilva");
    }

    @Test
    @DisplayName("Does not create notification when author comments on their own check-in")
    void shouldIgnoreCommentOnOwnCheckin() {
        Checkin checkin = Checkin.builder()
                .id(checkinId)
                .userId(checkinAuthorId)
                .build();

        when(checkinRepository.findById(checkinId)).thenReturn(Optional.of(checkin));

        CommentCreatedEvent event = new CommentCreatedEvent(
                UUID.randomUUID(),
                checkinId,
                checkinAuthorId, // same as checkin author
                UUID.randomUUID()
        );

        listener.onCommentCreated(event);

        verify(createInAppNotificationUseCase, never()).execute(any());
    }

    @Test
    @DisplayName("Creates notification when badge is granted")
    void shouldCreateNotificationOnBadgeGranted() {
        BadgeGrantedEvent event = new BadgeGrantedEvent(
                checkinAuthorId,
                UUID.randomUUID(),
                "7 Dias Seguidos",
                null
        );

        listener.onBadgeGranted(event);

        ArgumentCaptor<CreateNotificationCommand> captor = ArgumentCaptor.forClass(CreateNotificationCommand.class);
        verify(createInAppNotificationUseCase).execute(captor.capture());

        assertThat(captor.getValue().userId()).isEqualTo(checkinAuthorId);
        assertThat(captor.getValue().type()).isEqualTo(NotificationType.BADGE_GRANTED);
        assertThat(captor.getValue().body()).contains("7 Dias Seguidos");
    }

    @Test
    @DisplayName("Creates notification when challenge is completed")
    void shouldCreateNotificationOnChallengeCompleted() {
        ChallengeCompletedEvent event = new ChallengeCompletedEvent(
                UUID.randomUUID(),
                checkinAuthorId,
                UUID.randomUUID(),
                Instant.now()
        );

        listener.onChallengeCompleted(event);

        ArgumentCaptor<CreateNotificationCommand> captor = ArgumentCaptor.forClass(CreateNotificationCommand.class);
        verify(createInAppNotificationUseCase).execute(captor.capture());

        assertThat(captor.getValue().userId()).isEqualTo(checkinAuthorId);
        assertThat(captor.getValue().type()).isEqualTo(NotificationType.CHALLENGE_COMPLETED);
    }

    @Test
    @DisplayName("Creates notification when community join request is approved")
    void shouldCreateNotificationOnJoinRequestApproved() {
        CommunityJoinRequestReviewedEvent event = new CommunityJoinRequestReviewedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                checkinAuthorId,
                UUID.randomUUID(),
                CommunityJoinRequestStatus.APPROVED
        );

        listener.onCommunityJoinRequestReviewed(event);

        ArgumentCaptor<CreateNotificationCommand> captor = ArgumentCaptor.forClass(CreateNotificationCommand.class);
        verify(createInAppNotificationUseCase).execute(captor.capture());

        assertThat(captor.getValue().userId()).isEqualTo(checkinAuthorId);
        assertThat(captor.getValue().type()).isEqualTo(NotificationType.JOIN_REQUEST_APPROVED);
    }
}
