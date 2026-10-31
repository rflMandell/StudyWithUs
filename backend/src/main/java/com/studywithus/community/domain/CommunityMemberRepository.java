package com.studywithus.community.domain;

import com.studywithus.shared.domain.PagedResult;

import java.util.Optional;
import java.util.UUID;

public interface CommunityMemberRepository {
    CommunityMember save(CommunityMember member);
    Optional<CommunityMember> findByCommunityIdAndUserId(UUID communityId, UUID userId);
    boolean isUserActiveMember(UUID communityId, UUID userId);
    long countActiveMembers(UUID communityId);
    PagedResult<CommunityMember> findActiveMembers(UUID communityId, int page, int size);
}
