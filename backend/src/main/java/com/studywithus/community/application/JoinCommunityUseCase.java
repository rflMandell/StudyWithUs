package com.studywithus.community.application;

import com.studywithus.community.domain.Community;
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
public class JoinCommunityUseCase implements UseCase<JoinCommunityCommand, CommunityMember> {

    private final CommunityRepository communityRepository;
    private final CommunityMemberRepository communityMemberRepository;

    @Override
    @Transactional
    public CommunityMember execute(JoinCommunityCommand command) {
        Community community = communityRepository.findById(command.communityId())
                .filter(Community::isActive)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "COMMUNITY_NOT_FOUND", "Community not found"));

        if (!community.isPublic()) {
            throw new DomainException(
                    HttpStatus.BAD_REQUEST,
                    "COMMUNITY_PRIVATE_REQUIRES_JOIN_REQUEST",
                    "This community is private and requires an approved join request"
            );
        }

        Optional<CommunityMember> existingMemberOpt = communityMemberRepository.findByCommunityIdAndUserId(
                command.communityId(),
                command.userId()
        );

        if (existingMemberOpt.isPresent()) {
            CommunityMember existingMember = existingMemberOpt.get();

            if (existingMember.getStatus() == MemberStatus.ACTIVE) {
                throw new DomainException(HttpStatus.CONFLICT, "COMMUNITY_ALREADY_MEMBER", "User is already a member of this community");
            }

            if (existingMember.getStatus() == MemberStatus.BANNED) {
                throw new DomainException(HttpStatus.FORBIDDEN, "COMMUNITY_MEMBER_BANNED", "User is banned from this community");
            }

            // Reactivate membership
            existingMember.setStatus(MemberStatus.ACTIVE);
            existingMember.setRole(CommunityRole.MEMBER);
            existingMember.setJoinedAt(Instant.now());
            existingMember.setLeftAt(null);
            return communityMemberRepository.save(existingMember);
        }

        CommunityMember newMember = CommunityMember.builder()
                .communityId(command.communityId())
                .userId(command.userId())
                .role(CommunityRole.MEMBER)
                .status(MemberStatus.ACTIVE)
                .joinedAt(Instant.now())
                .build();

        return communityMemberRepository.save(newMember);
    }
}
