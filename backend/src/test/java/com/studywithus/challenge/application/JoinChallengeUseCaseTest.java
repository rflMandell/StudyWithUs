package com.studywithus.challenge.application;

import com.studywithus.challenge.domain.Challenge;
import com.studywithus.challenge.domain.ChallengeParticipantJoinedEvent;
import com.studywithus.challenge.domain.ChallengeParticipant;
import com.studywithus.challenge.domain.ChallengeRepository;
import com.studywithus.challenge.domain.ChallengeStatus;
import com.studywithus.community.domain.CommunityMemberRepository;
import com.studywithus.identity.domain.User;
import com.studywithus.identity.domain.UserRepository;
import com.studywithus.identity.domain.UserStatus;
import com.studywithus.shared.domain.DomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JoinChallengeUseCaseTest {

    @Mock
    private ChallengeRepository challengeRepository;

    @Mock
    private CommunityMemberRepository communityMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private JoinChallengeUseCase useCase;

    private UUID userId;
    private UUID challengeId;
    private UUID communityId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        challengeId = UUID.randomUUID();
        communityId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Successfully joins active challenge in community")
    void shouldJoinActiveChallenge() {
        User user = User.builder().id(userId).status(UserStatus.ACTIVE).build();
        Challenge challenge = Challenge.builder()
                .id(challengeId)
                .communityId(communityId)
                .targetValue(5)
                .status(ChallengeStatus.ACTIVE)
                .startAt(Instant.now().minus(1, ChronoUnit.DAYS))
                .endAt(Instant.now().plus(10, ChronoUnit.DAYS))
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(challengeRepository.findById(challengeId)).thenReturn(Optional.of(challenge));
        when(communityMemberRepository.isUserActiveMember(communityId, userId)).thenReturn(true);
        when(challengeRepository.findParticipant(challengeId, userId)).thenReturn(Optional.empty());
        when(challengeRepository.saveParticipant(any(ChallengeParticipant.class))).thenAnswer(inv -> inv.getArgument(0));

        ChallengeParticipantResult result = useCase.execute(new JoinChallengeCommand(challengeId, userId));

        assertThat(result).isNotNull();
        assertThat(result.challengeId()).isEqualTo(challengeId);
        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.progress()).isZero();
        assertThat(result.completed()).isFalse();
        verify(challengeRepository).saveParticipant(any(ChallengeParticipant.class));
        verify(eventPublisher).publishEvent(any(ChallengeParticipantJoinedEvent.class));
    }

    @Test
    @DisplayName("Idempotent join returns existing participation without saving new record")
    void shouldBeIdempotentWhenAlreadyJoined() {
        User user = User.builder().id(userId).status(UserStatus.ACTIVE).build();
        Challenge challenge = Challenge.builder()
                .id(challengeId)
                .communityId(communityId)
                .targetValue(5)
                .status(ChallengeStatus.ACTIVE)
                .build();

        ChallengeParticipant existing = ChallengeParticipant.builder()
                .challengeId(challengeId)
                .userId(userId)
                .progress(2)
                .joinedAt(Instant.now().minus(2, ChronoUnit.DAYS))
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(challengeRepository.findById(challengeId)).thenReturn(Optional.of(challenge));
        when(communityMemberRepository.isUserActiveMember(communityId, userId)).thenReturn(true);
        when(challengeRepository.findParticipant(challengeId, userId)).thenReturn(Optional.of(existing));

        ChallengeParticipantResult result = useCase.execute(new JoinChallengeCommand(challengeId, userId));

        assertThat(result).isNotNull();
        assertThat(result.progress()).isEqualTo(2);
        verify(challengeRepository, never()).saveParticipant(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Throws FORBIDDEN when user is not a member of the community")
    void shouldThrowForbiddenWhenNotMember() {
        User user = User.builder().id(userId).status(UserStatus.ACTIVE).build();
        Challenge challenge = Challenge.builder()
                .id(challengeId)
                .communityId(communityId)
                .targetValue(5)
                .status(ChallengeStatus.ACTIVE)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(challengeRepository.findById(challengeId)).thenReturn(Optional.of(challenge));
        when(communityMemberRepository.isUserActiveMember(communityId, userId)).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(new JoinChallengeCommand(challengeId, userId)))
                .isInstanceOf(DomainException.class)
                .matches(e -> ((DomainException) e).getStatus() == HttpStatus.FORBIDDEN);
    }
}
