package com.studywithus.community.application;

import com.studywithus.community.domain.Community;
import com.studywithus.community.domain.CommunityMember;
import com.studywithus.community.domain.CommunityMemberRepository;
import com.studywithus.community.domain.CommunityRepository;
import com.studywithus.community.domain.CommunityRole;
import com.studywithus.shared.domain.PagedResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ListCommunitiesUseCase {

    private final CommunityRepository communityRepository;
    private final CommunityMemberRepository communityMemberRepository;

    @Transactional(readOnly = true)
    public PagedResult<CommunityResult> execute(String search, int page, int size, UUID currentUserId) {
        PagedResult<Community> pagedCommunities = communityRepository.findPublicCommunities(search, page, size);

        List<CommunityResult> results = pagedCommunities.content().stream()
                .map(community -> {
                    long count = communityMemberRepository.countActiveMembers(community.getId());
                    CommunityRole userRole = null;
                    if (currentUserId != null) {
                        userRole = communityMemberRepository.findByCommunityIdAndUserId(community.getId(), currentUserId)
                                .filter(CommunityMember::isActive)
                                .map(CommunityMember::getRole)
                                .orElse(null);
                    }
                    return CommunityResult.from(community, count, userRole);
                })
                .toList();

        return new PagedResult<>(
                results,
                pagedCommunities.page(),
                pagedCommunities.size(),
                pagedCommunities.totalElements(),
                pagedCommunities.totalPages(),
                pagedCommunities.hasNext()
        );
    }
}
