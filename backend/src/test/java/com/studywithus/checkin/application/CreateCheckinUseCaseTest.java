package com.studywithus.checkin.application;

import com.studywithus.checkin.domain.Checkin;
import com.studywithus.checkin.domain.CheckinCreatedEvent;
import com.studywithus.checkin.domain.CheckinRepository;
import com.studywithus.community.domain.Community;
import com.studywithus.community.domain.CommunityMember;
import com.studywithus.community.domain.CommunityMemberRepository;
import com.studywithus.community.domain.CommunityRepository;
import com.studywithus.community.domain.CommunityRole;
import com.studywithus.community.domain.MemberStatus;
import com.studywithus.shared.domain.DomainException;
import com.studywithus.shared.infrastructure.IdempotencyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateCheckinUseCaseTest {

    @Mock
    private CheckinRepository checkinRepository;

    @Mock
    private CommunityRepository communityRepository;

    @Mock
    private CommunityMemberRepository communityMemberRepository;

    @Mock
    private IdempotencyService idempotencyService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private CreateCheckinUseCase createCheckinUseCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID communityId = UUID.randomUUID();

    @Test
    @DisplayName("Non-member cannot create check-in (throws 403 COMMUNITY_NOT_A_MEMBER)")
    void nonMemberCannotCreateCheckin() {
        when(idempotencyService.get(any(), any())).thenReturn(Optional.empty());
        when(communityRepository.findById(communityId)).thenReturn(Optional.of(Community.builder().id(communityId).build()));
        when(communityMemberRepository.findByCommunityIdAndUserId(communityId, userId)).thenReturn(Optional.empty());

        CreateCheckinCommand command = new CreateCheckinCommand(userId, communityId, "Estudei 2 horas", List.of("key1"), "key-123");

        assertThatThrownBy(() -> createCheckinUseCase.execute(command))
                .isInstanceOf(DomainException.class)
                .satisfies(e -> {
                    DomainException de = (DomainException) e;
                    assertThat(de.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(de.getCode()).isEqualTo("COMMUNITY_NOT_A_MEMBER");
                });
    }

    @Test
    @DisplayName("Active member creates check-in successfully and event is published")
    void activeMemberCreatesCheckin() {
        Community community = Community.builder().id(communityId).build();
        CommunityMember member = CommunityMember.builder()
                .communityId(communityId)
                .userId(userId)
                .role(CommunityRole.MEMBER)
                .status(MemberStatus.ACTIVE)
                .build();

        when(idempotencyService.get(any(), any())).thenReturn(Optional.empty());
        when(communityRepository.findById(communityId)).thenReturn(Optional.of(community));
        when(communityMemberRepository.findByCommunityIdAndUserId(communityId, userId)).thenReturn(Optional.of(member));
        when(checkinRepository.save(any(Checkin.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateCheckinCommand command = new CreateCheckinCommand(userId, communityId, "Estudo de Grafos", List.of("img1.jpg"), "idem-1");

        CheckinResult result = createCheckinUseCase.execute(command);

        assertThat(result.caption()).isEqualTo("Estudo de Grafos");
        assertThat(result.mediaKeys()).containsExactly("img1.jpg");
        verify(eventPublisher).publishEvent(any(CheckinCreatedEvent.class));
        verify(idempotencyService).set(any(), any(), any(Long.class));
    }
}
