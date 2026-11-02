package com.studywithus.checkin.application;

import com.studywithus.checkin.domain.Checkin;
import com.studywithus.checkin.domain.CheckinDeletedEvent;
import com.studywithus.checkin.domain.CheckinRepository;
import com.studywithus.community.domain.CommunityMember;
import com.studywithus.community.domain.CommunityMemberRepository;
import com.studywithus.community.domain.CommunityRole;
import com.studywithus.community.domain.MemberStatus;
import com.studywithus.shared.domain.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteCheckinUseCaseTest {

    @Mock
    private CheckinRepository checkinRepository;

    @Mock
    private CommunityMemberRepository communityMemberRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private DeleteCheckinUseCase deleteCheckinUseCase;

    private final UUID checkinId = UUID.randomUUID();
    private final UUID authorId = UUID.randomUUID();
    private final UUID communityId = UUID.randomUUID();
    private final UUID otherUserId = UUID.randomUUID();

    @Test
    @DisplayName("Author deletes own checkin successfully (soft delete)")
    void authorDeletesOwnCheckin() {
        Checkin checkin = Checkin.builder()
                .id(checkinId)
                .userId(authorId)
                .communityId(communityId)
                .caption("Meu checkin")
                .build();

        when(checkinRepository.findById(checkinId)).thenReturn(Optional.of(checkin));

        deleteCheckinUseCase.execute(new DeleteCheckinCommand(checkinId, authorId));

        assertThat(checkin.isActive()).isFalse();
        assertThat(checkin.getDeletedAt()).isNotNull();
        verify(checkinRepository).save(checkin);
        verify(eventPublisher).publishEvent(any(CheckinDeletedEvent.class));
    }

    @Test
    @DisplayName("Non-author member cannot delete checkin (throws 403 CHECKIN_ACCESS_DENIED)")
    void otherMemberCannotDeleteCheckin() {
        Checkin checkin = Checkin.builder()
                .id(checkinId)
                .userId(authorId)
                .communityId(communityId)
                .caption("Meu checkin")
                .build();

        CommunityMember regularMember = CommunityMember.builder()
                .communityId(communityId)
                .userId(otherUserId)
                .role(CommunityRole.MEMBER)
                .status(MemberStatus.ACTIVE)
                .build();

        when(checkinRepository.findById(checkinId)).thenReturn(Optional.of(checkin));
        when(communityMemberRepository.findByCommunityIdAndUserId(communityId, otherUserId))
                .thenReturn(Optional.of(regularMember));

        assertThatThrownBy(() -> deleteCheckinUseCase.execute(new DeleteCheckinCommand(checkinId, otherUserId)))
                .isInstanceOf(DomainException.class)
                .satisfies(e -> {
                    DomainException de = (DomainException) e;
                    assertThat(de.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(de.getCode()).isEqualTo("CHECKIN_ACCESS_DENIED");
                });
    }

    @Test
    @DisplayName("Community moderator/admin can delete other member's checkin")
    void moderatorCanDeleteCheckin() {
        Checkin checkin = Checkin.builder()
                .id(checkinId)
                .userId(authorId)
                .communityId(communityId)
                .caption("Meu checkin")
                .build();

        CommunityMember moderator = CommunityMember.builder()
                .communityId(communityId)
                .userId(otherUserId)
                .role(CommunityRole.MODERATOR)
                .status(MemberStatus.ACTIVE)
                .build();

        when(checkinRepository.findById(checkinId)).thenReturn(Optional.of(checkin));
        when(communityMemberRepository.findByCommunityIdAndUserId(communityId, otherUserId))
                .thenReturn(Optional.of(moderator));

        deleteCheckinUseCase.execute(new DeleteCheckinCommand(checkinId, otherUserId));

        assertThat(checkin.isActive()).isFalse();
        verify(checkinRepository).save(checkin);
        verify(eventPublisher).publishEvent(any(CheckinDeletedEvent.class));
    }
}
