package com.studywithus.community.domain;

import com.studywithus.shared.domain.PagedResult;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface CommunityMemberRepository {
    CommunityMember save(CommunityMember member);
    Optional<CommunityMember> findByCommunityIdAndUserId(UUID communityId, UUID userId);
    boolean isUserActiveMember(UUID communityId, UUID userId);
    long countActiveMembers(UUID communityId);
    Map<UUID, Long> countActiveMembersByCommunityIds(Collection<UUID> communityIds);
    List<CommunityMember> findActiveByCommunityIdsAndUserId(Collection<UUID> communityIds, UUID userId);
    PagedResult<CommunityMember> findActiveMembers(UUID communityId, int page, int size);
    java.util.List<UUID> findActiveCommunityIdsByUserId(UUID userId);
}
