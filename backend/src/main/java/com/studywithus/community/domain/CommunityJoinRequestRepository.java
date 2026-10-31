package com.studywithus.community.domain;

import com.studywithus.shared.domain.PagedResult;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CommunityJoinRequestRepository {
    CommunityJoinRequest save(CommunityJoinRequest request);
    Optional<CommunityJoinRequest> findById(UUID id);
    Optional<CommunityJoinRequest> findPendingByCommunityIdAndUserId(UUID communityId, UUID userId);
    PagedResult<CommunityJoinRequest> findByCommunityIdAndStatus(UUID communityId, CommunityJoinRequestStatus status, int page, int size);
    List<CommunityJoinRequest> findByCommunityIdAndStatus(UUID communityId, CommunityJoinRequestStatus status);
}
