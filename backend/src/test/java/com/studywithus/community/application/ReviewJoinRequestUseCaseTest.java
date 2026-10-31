package com.studywithus.community.application;

import com.studywithus.community.domain.Community;
import com.studywithus.community.domain.CommunityJoinRequest;
import com.studywithus.community.domain.CommunityJoinRequestRepository;
import com.studywithus.community.domain.CommunityJoinRequestStatus;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewJoinRequestUseCaseTest {

    @Mock
    private CommunityRepository communityRepository;

    @Mock
    private CommunityMemberRepository communityMemberRepository;

    @Mock
    private CommunityJoinRequestRepository joinRequestRepository;

    @InjectMocks
    private ReviewJoinRequestUseCase reviewJoinRequestUseCase;

    private final UUID communityId = UUID.randomUUID();
    private final UUID adminId = UUID.randomUUID();
    private final UUID applicantId = UUID.randomUUID();
    private final UUID requestId = UUID.randomUUID();

    @Test
    @DisplayName("Non-admin cannot review join requests (throws 403)")
    void nonAdminCannotReview() {
        UUID regularUserId = UUID.randomUUID();
        CommunityMember member = CommunityMember.builder()
                .communityId(communityId)
                .userId(regularUserId)
                .role(CommunityRole.MEMBER)
                .status(MemberStatus.ACTIVE)
                .build();

        when(communityMemberRepository.findByCommunityIdAndUserId(communityId, regularUserId))
                .thenReturn(Optional.of(member));

        ReviewJoinRequestCommand command = new ReviewJoinRequestCommand(
                communityId,
                requestId,
                regularUserId,
                true
        );

        assertThatThrownBy(() -> reviewJoinRequestUseCase.execute(command))
                .isInstanceOf(DomainException.class)
                .satisfies(e -> {
                    DomainException de = (DomainException) e;
                    assertThat(de.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(de.getCode()).isEqualTo("COMMUNITY_ACCESS_DENIED");
                });
    }

    @Test
    @DisplayName("Admin approves join request: creates active membership and marks request APPROVED")
    void adminApprovesJoinRequest() {
        CommunityMember admin = CommunityMember.builder()
                .communityId(communityId)
                .userId(adminId)
                .role(CommunityRole.ADMIN)
                .status(MemberStatus.ACTIVE)
                .build();

        CommunityJoinRequest joinRequest = CommunityJoinRequest.builder()
                .id(requestId)
                .communityId(communityId)
                .userId(applicantId)
                .status(CommunityJoinRequestStatus.PENDING)
                .createdAt(Instant.now())
                .build();

        Community community = Community.builder().id(communityId).build();

        when(communityMemberRepository.findByCommunityIdAndUserId(communityId, adminId))
                .thenReturn(Optional.of(admin));
        when(joinRequestRepository.findById(requestId))
                .thenReturn(Optional.of(joinRequest));
        when(communityRepository.findById(communityId))
                .thenReturn(Optional.of(community));
        when(communityMemberRepository.findByCommunityIdAndUserId(communityId, applicantId))
                .thenReturn(Optional.empty());
        when(joinRequestRepository.save(any(CommunityJoinRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReviewJoinRequestCommand command = new ReviewJoinRequestCommand(
                communityId,
                requestId,
                adminId,
                true
        );

        CommunityJoinRequestResult result = reviewJoinRequestUseCase.execute(command);

        assertThat(result.status()).isEqualTo(CommunityJoinRequestStatus.APPROVED);
        assertThat(result.reviewedBy()).isEqualTo(adminId);
        verify(communityMemberRepository).save(any(CommunityMember.class));
        verify(joinRequestRepository).save(joinRequest);
    }

    @Test
    @DisplayName("Admin rejects join request: marks request REJECTED without creating member")
    void adminRejectsJoinRequest() {
        CommunityMember admin = CommunityMember.builder()
                .communityId(communityId)
                .userId(adminId)
                .role(CommunityRole.ADMIN)
                .status(MemberStatus.ACTIVE)
                .build();

        CommunityJoinRequest joinRequest = CommunityJoinRequest.builder()
                .id(requestId)
                .communityId(communityId)
                .userId(applicantId)
                .status(CommunityJoinRequestStatus.PENDING)
                .createdAt(Instant.now())
                .build();

        when(communityMemberRepository.findByCommunityIdAndUserId(communityId, adminId))
                .thenReturn(Optional.of(admin));
        when(joinRequestRepository.findById(requestId))
                .thenReturn(Optional.of(joinRequest));
        when(joinRequestRepository.save(any(CommunityJoinRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReviewJoinRequestCommand command = new ReviewJoinRequestCommand(
                communityId,
                requestId,
                adminId,
                false
        );

        CommunityJoinRequestResult result = reviewJoinRequestUseCase.execute(command);

        assertThat(result.status()).isEqualTo(CommunityJoinRequestStatus.REJECTED);
        assertThat(result.reviewedBy()).isEqualTo(adminId);
        verify(communityMemberRepository, never()).save(any(CommunityMember.class));
        verify(joinRequestRepository).save(joinRequest);
    }
}
