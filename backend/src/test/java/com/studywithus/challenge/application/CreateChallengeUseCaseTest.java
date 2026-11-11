package com.studywithus.challenge.application;

import com.studywithus.challenge.domain.Challenge;
import com.studywithus.challenge.domain.ChallengeCreatedEvent;
import com.studywithus.challenge.domain.ChallengeMetric;
import com.studywithus.challenge.domain.ChallengeRepository;
import com.studywithus.challenge.domain.ChallengeScope;
import com.studywithus.challenge.domain.RewardType;
import com.studywithus.community.domain.Community;
import com.studywithus.community.domain.CommunityMember;
import com.studywithus.community.domain.CommunityMemberRepository;
import com.studywithus.community.domain.CommunityRepository;
import com.studywithus.community.domain.CommunityRole;
import com.studywithus.community.domain.MemberStatus;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateChallengeUseCaseTest {

    @Mock
    private ChallengeRepository challengeRepository;

    @Mock
    private CommunityRepository communityRepository;

    @Mock
    private CommunityMemberRepository communityMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private CreateChallengeUseCase useCase;

    private UUID creatorId;
    private UUID communityId;

    @BeforeEach
    void setUp() {
        creatorId = UUID.randomUUID();
        communityId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Successfully creates challenge when creator is community owner/admin")
    void shouldCreateChallengeSuccessfully() {
        User creator = User.builder().id(creatorId).status(UserStatus.ACTIVE).build();
        Community community = Community.builder().id(communityId).name("Java Devs").build();
        CommunityMember member = CommunityMember.builder()
                .communityId(communityId)
                .userId(creatorId)
                .role(CommunityRole.OWNER)
                .status(MemberStatus.ACTIVE)
                .build();

        when(userRepository.findById(creatorId)).thenReturn(Optional.of(creator));
        when(communityRepository.findById(communityId)).thenReturn(Optional.of(community));
        when(communityMemberRepository.findByCommunityIdAndUserId(communityId, creatorId)).thenReturn(Optional.of(member));
        when(challengeRepository.save(any(Challenge.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateChallengeCommand command = new CreateChallengeCommand(
                creatorId,
                communityId,
                "Desafio 10 Checkins",
                "Estude 10 vezes",
                ChallengeScope.INDIVIDUAL,
                ChallengeMetric.CHECKINS,
                10,
                Instant.now(),
                Instant.now().plus(30, ChronoUnit.DAYS),
                List.of(new CreateRewardCommand(RewardType.POINTS, 50, null))
        );

        ChallengeResult result = useCase.execute(command);

        assertThat(result).isNotNull();
        assertThat(result.title()).isEqualTo("Desafio 10 Checkins");
        assertThat(result.targetValue()).isEqualTo(10);
        assertThat(result.rewards()).hasSize(1);
        assertThat(result.rewards().get(0).value()).isEqualTo(50);
        verify(challengeRepository).save(any(Challenge.class));
        verify(eventPublisher).publishEvent(any(ChallengeCreatedEvent.class));
    }

    @Test
    @DisplayName("Throws FORBIDDEN when user is not an admin in the community")
    void shouldThrowForbiddenWhenNotAdmin() {
        User creator = User.builder().id(creatorId).status(UserStatus.ACTIVE).build();
        Community community = Community.builder().id(communityId).name("Java Devs").build();
        CommunityMember regularMember = CommunityMember.builder()
                .communityId(communityId)
                .userId(creatorId)
                .role(CommunityRole.MEMBER)
                .status(MemberStatus.ACTIVE)
                .build();

        when(userRepository.findById(creatorId)).thenReturn(Optional.of(creator));
        when(communityRepository.findById(communityId)).thenReturn(Optional.of(community));
        when(communityMemberRepository.findByCommunityIdAndUserId(communityId, creatorId)).thenReturn(Optional.of(regularMember));

        CreateChallengeCommand command = new CreateChallengeCommand(
                creatorId,
                communityId,
                "Desafio",
                "Desc",
                ChallengeScope.INDIVIDUAL,
                ChallengeMetric.CHECKINS,
                5,
                null,
                null,
                List.of()
        );

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(DomainException.class)
                .matches(e -> ((DomainException) e).getStatus() == HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("Throws BAD_REQUEST when target value is zero or negative")
    void shouldThrowBadRequestWhenTargetValueZero() {
        User creator = User.builder().id(creatorId).status(UserStatus.ACTIVE).build();
        when(userRepository.findById(creatorId)).thenReturn(Optional.of(creator));

        CreateChallengeCommand command = new CreateChallengeCommand(
                creatorId,
                communityId,
                "Desafio",
                "Desc",
                ChallengeScope.INDIVIDUAL,
                ChallengeMetric.CHECKINS,
                0,
                null,
                null,
                List.of()
        );

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(DomainException.class)
                .matches(e -> ((DomainException) e).getStatus() == HttpStatus.BAD_REQUEST);
    }
}
