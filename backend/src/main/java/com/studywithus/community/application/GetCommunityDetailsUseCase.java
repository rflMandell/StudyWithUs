package com.studywithus.community.application;

import com.studywithus.community.domain.Community;
import com.studywithus.community.domain.CommunityMember;
import com.studywithus.community.domain.CommunityMemberRepository;
import com.studywithus.community.domain.CommunityRepository;
import com.studywithus.community.domain.CommunityRole;
import com.studywithus.shared.domain.DomainException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetCommunityDetailsUseCase {

    private final CommunityRepository communityRepository;
    private final CommunityMemberRepository communityMemberRepository;

    @Transactional(readOnly = true)
    public CommunityResult execute(UUID communityId, UUID currentUserId) {
        Community community = communityRepository.findById(communityId)
                .filter(Community::isActive)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "COMMUNITY_NOT_FOUND", "Community not found"));

        long membersCount = communityMemberRepository.countActiveMembers(communityId);

        CommunityRole currentUserRole = null;
        if (currentUserId != null) {
            currentUserRole = communityMemberRepository.findByCommunityIdAndUserId(communityId, currentUserId)
                    .filter(CommunityMember::isActive)
                    .map(CommunityMember::getRole)
                    .orElse(null);
        }

        return CommunityResult.from(community, membersCount, currentUserRole);
    }
}
