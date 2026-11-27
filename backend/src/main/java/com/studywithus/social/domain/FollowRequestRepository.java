package com.studywithus.social.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FollowRequestRepository {
    FollowRequest save(FollowRequest followRequest);
    Optional<FollowRequest> findById(UUID id);
    Optional<FollowRequest> findByRequesterIdAndTargetId(UUID requesterId, UUID targetId);
    List<FollowRequest> findPendingByTargetId(UUID targetId);
    void deleteByRequesterIdAndTargetId(UUID requesterId, UUID targetId);
}
