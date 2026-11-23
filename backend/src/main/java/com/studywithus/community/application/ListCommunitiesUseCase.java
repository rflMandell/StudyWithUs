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
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ListCommunitiesUseCase {

    private final CommunityRepository communityRepository;
    private final CommunityMemberRepository communityMemberRepository;

    @Transactional(readOnly = true)
    public PagedResult<CommunityResult> execute(String search, int page, int size, UUID currentUserId) {
        PagedResult<Community> pagedCommunities = communityRepository.findPublicCommunities(search, page, size);
        List<Community> communities = pagedCommunities.content();

        List<UUID> communityIds = communities.stream().map(Community::getId).toList();
        Map<UUID, Long> memberCounts = communityMemberRepository.countActiveMembersByCommunityIds(communityIds);
        Map<UUID, CommunityRole> userRoles = currentUserId == null || communityIds.isEmpty()
                ? Map.of()
                : communityMemberRepository.findActiveByCommunityIdsAndUserId(communityIds, currentUserId).stream()
                        .collect(Collectors.toMap(CommunityMember::getCommunityId, CommunityMember::getRole));

        List<CommunityResult> results = communities.stream()
                .map(community -> CommunityResult.from(
                        community,
                        memberCounts.getOrDefault(community.getId(), 0L),
                        userRoles.get(community.getId())
                ))
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
