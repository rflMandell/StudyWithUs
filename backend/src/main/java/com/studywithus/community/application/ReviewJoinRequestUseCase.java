package com.studywithus.community.application;

import com.studywithus.community.domain.Community;
import com.studywithus.community.domain.CommunityJoinRequest;
import com.studywithus.community.domain.CommunityJoinRequestRepository;
import com.studywithus.community.domain.CommunityMember;
import com.studywithus.community.domain.CommunityMemberRepository;
import com.studywithus.community.domain.CommunityRepository;
import com.studywithus.community.domain.CommunityRole;
import com.studywithus.community.domain.MemberStatus;
import com.studywithus.shared.application.UseCase;
import com.studywithus.shared.domain.DomainException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReviewJoinRequestUseCase implements UseCase<ReviewJoinRequestCommand, CommunityJoinRequestResult> {

    private final CommunityRepository communityRepository;
    private final CommunityMemberRepository communityMemberRepository;
    private final CommunityJoinRequestRepository joinRequestRepository;

    @Override
    @Transactional
    public CommunityJoinRequestResult execute(ReviewJoinRequestCommand command) {
        CommunityMember reviewer = communityMemberRepository.findByCommunityIdAndUserId(
                command.communityId(),
                command.reviewerUserId()
        ).orElseThrow(() -> new DomainException(HttpStatus.FORBIDDEN, "COMMUNITY_ACCESS_DENIED", "Reviewer is not a member of this community"));

        if (!reviewer.isAdmin()) {
            throw new DomainException(HttpStatus.FORBIDDEN, "COMMUNITY_ACCESS_DENIED", "Only community admins or owner can review join requests");
        }

        CommunityJoinRequest request = joinRequestRepository.findById(command.requestId())
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "JOIN_REQUEST_NOT_FOUND", "Join request not found"));

        if (!request.getCommunityId().equals(command.communityId())) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "JOIN_REQUEST_MISMATCH", "Join request does not belong to this community");
        }

        if (command.approved()) {
            request.approve(command.reviewerUserId());

            communityRepository.findById(command.communityId())
                    .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "COMMUNITY_NOT_FOUND", "Community not found"));

            Optional<CommunityMember> existingMemberOpt = communityMemberRepository.findByCommunityIdAndUserId(
                    command.communityId(),
                    request.getUserId()
            );

            CommunityMember memberToSave;
            if (existingMemberOpt.isPresent()) {
                memberToSave = existingMemberOpt.get();
                memberToSave.setStatus(MemberStatus.ACTIVE);
                memberToSave.setRole(CommunityRole.MEMBER);
                memberToSave.setJoinedAt(Instant.now());
                memberToSave.setLeftAt(null);
            } else {
                memberToSave = CommunityMember.builder()
                        .communityId(command.communityId())
                        .userId(request.getUserId())
                        .role(CommunityRole.MEMBER)
                        .status(MemberStatus.ACTIVE)
                        .joinedAt(Instant.now())
                        .build();
            }

            communityMemberRepository.save(memberToSave);
        } else {
            request.reject(command.reviewerUserId());
        }

        CommunityJoinRequest saved = joinRequestRepository.save(request);
        return CommunityJoinRequestResult.fromDomain(saved);
    }
}
