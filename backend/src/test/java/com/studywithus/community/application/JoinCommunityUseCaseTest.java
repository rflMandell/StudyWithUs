package com.studywithus.community.application;

import com.studywithus.community.domain.Community;
import com.studywithus.community.domain.CommunityMember;
import com.studywithus.community.domain.CommunityMemberRepository;
import com.studywithus.community.domain.CommunityRepository;
import com.studywithus.community.domain.CommunityRole;
import com.studywithus.community.domain.CommunityVisibility;
import com.studywithus.community.domain.MemberStatus;
import com.studywithus.shared.domain.DomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JoinCommunityUseCaseTest {

    @Mock
    private CommunityRepository communityRepository;

    @Mock
    private CommunityMemberRepository communityMemberRepository;

    @InjectMocks
    private JoinCommunityUseCase joinCommunityUseCase;

    private UUID communityId;
    private UUID userId;
    private Community publicCommunity;

    @BeforeEach
    void setUp() {
        communityId = UUID.randomUUID();
        userId = UUID.randomUUID();
        publicCommunity = Community.builder()
                .id(communityId)
                .name("Java Developers")
                .visibility(CommunityVisibility.PUBLIC)
                .build();
    }

    @Test
    @DisplayName("Should successfully join a public community")
    void joinPublicCommunitySuccessfully() {
        when(communityRepository.findById(communityId)).thenReturn(Optional.of(publicCommunity));
        when(communityMemberRepository.findByCommunityIdAndUserId(communityId, userId)).thenReturn(Optional.empty());
        when(communityMemberRepository.save(any(CommunityMember.class))).thenAnswer(inv -> inv.getArgument(0));

        CommunityMember member = joinCommunityUseCase.execute(new JoinCommunityCommand(communityId, userId));

        assertThat(member).isNotNull();
        assertThat(member.getCommunityId()).isEqualTo(communityId);
        assertThat(member.getUserId()).isEqualTo(userId);
        assertThat(member.getRole()).isEqualTo(CommunityRole.MEMBER);
        assertThat(member.getStatus()).isEqualTo(MemberStatus.ACTIVE);

        verify(communityMemberRepository).save(any(CommunityMember.class));
    }

    @Test
    @DisplayName("Should reject joining a private community without request")
    void throwWhenJoiningPrivateCommunity() {
        publicCommunity.setVisibility(CommunityVisibility.PRIVATE);
        when(communityRepository.findById(communityId)).thenReturn(Optional.of(publicCommunity));

        assertThatThrownBy(() -> joinCommunityUseCase.execute(new JoinCommunityCommand(communityId, userId)))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> {
                    DomainException de = (DomainException) ex;
                    assertThat(de.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(de.getCode()).isEqualTo("COMMUNITY_PRIVATE_REQUIRES_JOIN_REQUEST");
                });
    }

    @Test
    @DisplayName("Should throw conflict when user is already active member")
    void throwWhenAlreadyMember() {
        CommunityMember existing = CommunityMember.builder()
                .communityId(communityId)
                .userId(userId)
                .status(MemberStatus.ACTIVE)
                .build();

        when(communityRepository.findById(communityId)).thenReturn(Optional.of(publicCommunity));
        when(communityMemberRepository.findByCommunityIdAndUserId(communityId, userId)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> joinCommunityUseCase.execute(new JoinCommunityCommand(communityId, userId)))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> {
                    DomainException de = (DomainException) ex;
                    assertThat(de.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(de.getCode()).isEqualTo("COMMUNITY_ALREADY_MEMBER");
                });
    }
}
