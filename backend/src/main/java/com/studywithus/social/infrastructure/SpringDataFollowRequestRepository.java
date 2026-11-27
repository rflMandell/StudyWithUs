package com.studywithus.social.infrastructure;

import com.studywithus.social.domain.FollowRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataFollowRequestRepository extends JpaRepository<FollowRequestJpaEntity, UUID> {
    Optional<FollowRequestJpaEntity> findByRequesterIdAndTargetId(UUID requesterId, UUID targetId);
    List<FollowRequestJpaEntity> findByTargetIdAndStatusOrderByCreatedAtDesc(UUID targetId, FollowRequestStatus status);
    void deleteByRequesterIdAndTargetId(UUID requesterId, UUID targetId);
}
