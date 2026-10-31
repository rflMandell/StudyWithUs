package com.studywithus.community.application;

import com.studywithus.community.domain.Community;
import com.studywithus.community.domain.CommunityMember;
import com.studywithus.community.domain.CommunityMemberRepository;
import com.studywithus.community.domain.CommunityRepository;
import com.studywithus.community.domain.CommunityRole;
import com.studywithus.community.domain.MemberStatus;
import com.studywithus.shared.domain.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
class UpdateMemberRoleUseCaseTest {

    @Mock
    private CommunityRepository communityRepository;

    @Mock
    private CommunityMemberRepository communityMemberRepository;

    @InjectMocks
    private UpdateMemberRoleUseCase updateMemberRoleUseCase;

    private final UUID communityId = UUID.randomUUID();
    private final UUID ownerId = UUID.randomUUID();
    private final UUID adminId = UUID.randomUUID();
    private final UUID memberId = UUID.randomUUID();

    @Test
    @DisplayName("Non-admin member cannot update roles (throws 403)")
    void nonAdminCannotUpdateRoles() {
        CommunityMember normalMember = CommunityMember.builder()
                .communityId(communityId)
                .userId(memberId)
                .role(CommunityRole.MEMBER)
                .status(MemberStatus.ACTIVE)
                .build();

        when(communityMemberRepository.findByCommunityIdAndUserId(communityId, memberId))
                .thenReturn(Optional.of(normalMember));

        UpdateMemberRoleCommand command = new UpdateMemberRoleCommand(
                communityId,
                UUID.randomUUID(),
                memberId,
                CommunityRole.MODERATOR
        );

        assertThatThrownBy(() -> updateMemberRoleUseCase.execute(command))
                .isInstanceOf(DomainException.class)
                .satisfies(e -> {
                    DomainException de = (DomainException) e;
                    assertThat(de.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(de.getCode()).isEqualTo("COMMUNITY_ACCESS_DENIED");
                });
    }

    @Test
    @DisplayName("Admin cannot update role of Owner (throws 403)")
    void adminCannotUpdateOwnerRole() {
        CommunityMember admin = CommunityMember.builder()
                .communityId(communityId)
                .userId(adminId)
                .role(CommunityRole.ADMIN)
                .status(MemberStatus.ACTIVE)
                .build();

        CommunityMember owner = CommunityMember.builder()
                .communityId(communityId)
                .userId(ownerId)
                .role(CommunityRole.OWNER)
                .status(MemberStatus.ACTIVE)
                .build();

        when(communityMemberRepository.findByCommunityIdAndUserId(communityId, adminId))
                .thenReturn(Optional.of(admin));
        when(communityMemberRepository.findByCommunityIdAndUserId(communityId, ownerId))
                .thenReturn(Optional.of(owner));

        UpdateMemberRoleCommand command = new UpdateMemberRoleCommand(
                communityId,
                ownerId,
                adminId,
                CommunityRole.MEMBER
        );

        assertThatThrownBy(() -> updateMemberRoleUseCase.execute(command))
                .isInstanceOf(DomainException.class)
                .satisfies(e -> {
                    DomainException de = (DomainException) e;
                    assertThat(de.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(de.getCode()).isEqualTo("COMMUNITY_ACCESS_DENIED");
                });
    }

    @Test
    @DisplayName("Admin promotes Member to Moderator (success)")
    void adminPromotesMemberToModerator() {
        CommunityMember admin = CommunityMember.builder()
                .communityId(communityId)
                .userId(adminId)
                .role(CommunityRole.ADMIN)
                .status(MemberStatus.ACTIVE)
                .build();

        CommunityMember targetMember = CommunityMember.builder()
                .communityId(communityId)
                .userId(memberId)
                .role(CommunityRole.MEMBER)
                .status(MemberStatus.ACTIVE)
                .joinedAt(Instant.now())
                .build();

        when(communityMemberRepository.findByCommunityIdAndUserId(communityId, adminId))
                .thenReturn(Optional.of(admin));
        when(communityMemberRepository.findByCommunityIdAndUserId(communityId, memberId))
                .thenReturn(Optional.of(targetMember));
        when(communityMemberRepository.save(any(CommunityMember.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UpdateMemberRoleCommand command = new UpdateMemberRoleCommand(
                communityId,
                memberId,
                adminId,
                CommunityRole.MODERATOR
        );

        CommunityMemberResult result = updateMemberRoleUseCase.execute(command);

        assertThat(result.role()).isEqualTo(CommunityRole.MODERATOR);
        assertThat(targetMember.getRole()).isEqualTo(CommunityRole.MODERATOR);
        verify(communityMemberRepository).save(targetMember);
    }

    @Test
    @DisplayName("Owner transfers ownership to Member (Owner becomes Admin, Target becomes Owner)")
    void ownerTransfersOwnership() {
        CommunityMember owner = CommunityMember.builder()
                .communityId(communityId)
                .userId(ownerId)
                .role(CommunityRole.OWNER)
                .status(MemberStatus.ACTIVE)
                .build();

        CommunityMember target = CommunityMember.builder()
                .communityId(communityId)
                .userId(memberId)
                .role(CommunityRole.MEMBER)
                .status(MemberStatus.ACTIVE)
                .joinedAt(Instant.now())
                .build();

        Community community = Community.builder()
                .id(communityId)
                .ownerId(ownerId)
                .build();

        when(communityMemberRepository.findByCommunityIdAndUserId(communityId, ownerId))
                .thenReturn(Optional.of(owner));
        when(communityMemberRepository.findByCommunityIdAndUserId(communityId, memberId))
                .thenReturn(Optional.of(target));
        when(communityRepository.findById(communityId))
                .thenReturn(Optional.of(community));
        when(communityMemberRepository.save(any(CommunityMember.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UpdateMemberRoleCommand command = new UpdateMemberRoleCommand(
                communityId,
                memberId,
                ownerId,
                CommunityRole.OWNER
        );

        CommunityMemberResult result = updateMemberRoleUseCase.execute(command);

        assertThat(result.role()).isEqualTo(CommunityRole.OWNER);
        assertThat(owner.getRole()).isEqualTo(CommunityRole.ADMIN);
        assertThat(target.getRole()).isEqualTo(CommunityRole.OWNER);
        assertThat(community.getOwnerId()).isEqualTo(memberId);
        verify(communityRepository).save(community);
    }
}
