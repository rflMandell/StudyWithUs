package com.studywithus.community.application;

import com.studywithus.community.domain.Community;
import com.studywithus.community.domain.CommunityJoinRequest;
import com.studywithus.community.domain.CommunityJoinRequestRepository;
import com.studywithus.community.domain.CommunityMember;
import com.studywithus.community.domain.CommunityMemberRepository;
import com.studywithus.community.domain.CommunityRepository;
import com.studywithus.community.domain.MemberStatus;
import com.studywithus.shared.application.UseCase;
import com.studywithus.shared.domain.DomainException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RequestJoinCommunityUseCase implements UseCase<RequestJoinCommunityCommand, CommunityJoinRequestResult> {

    private final CommunityRepository communityRepository;
    private final CommunityMemberRepository communityMemberRepository;
    private final CommunityJoinRequestRepository joinRequestRepository;

    @Override
    @Transactional
    public CommunityJoinRequestResult execute(RequestJoinCommunityCommand command) {
        Community community = communityRepository.findById(command.communityId())
                .filter(Community::isActive)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "COMMUNITY_NOT_FOUND", "Community not found"));

        if (community.isPublic()) {
            throw new DomainException(
                    HttpStatus.BAD_REQUEST,
                    "COMMUNITY_IS_PUBLIC",
                    "Public communities do not require a join request. Join directly via POST /communities/{id}/members"
            );
        }

        Optional<CommunityMember> existingMemberOpt = communityMemberRepository.findByCommunityIdAndUserId(
                command.communityId(),
                command.userId()
        );

        if (existingMemberOpt.isPresent()) {
            CommunityMember member = existingMemberOpt.get();
            if (member.getStatus() == MemberStatus.ACTIVE) {
                throw new DomainException(HttpStatus.CONFLICT, "COMMUNITY_ALREADY_MEMBER", "User is already an active member of this community");
            }
            if (member.getStatus() == MemberStatus.BANNED) {
                throw new DomainException(HttpStatus.FORBIDDEN, "COMMUNITY_MEMBER_BANNED", "User is banned from this community");
            }
        }

        Optional<CommunityJoinRequest> pendingRequest = joinRequestRepository.findPendingByCommunityIdAndUserId(
                command.communityId(),
                command.userId()
        );

        if (pendingRequest.isPresent()) {
            throw new DomainException(
                    HttpStatus.CONFLICT,
                    "JOIN_REQUEST_ALREADY_PENDING",
                    "A join request is already pending review for this community"
            );
        }

        CommunityJoinRequest request = CommunityJoinRequest.create(command.communityId(), command.userId());
        CommunityJoinRequest saved = joinRequestRepository.save(request);

        return CommunityJoinRequestResult.fromDomain(saved);
    }
}
