package com.studywithus.community.infrastructure;

import com.studywithus.community.domain.CommunityJoinRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataCommunityJoinRequestRepository extends JpaRepository<CommunityJoinRequestJpaEntity, UUID> {

    @Query("SELECT r FROM CommunityJoinRequestJpaEntity r WHERE r.communityId = :communityId AND r.userId = :userId AND r.status = 'PENDING'")
    Optional<CommunityJoinRequestJpaEntity> findPendingByCommunityIdAndUserId(
            @Param("communityId") UUID communityId,
            @Param("userId") UUID userId
    );

    Page<CommunityJoinRequestJpaEntity> findByCommunityIdAndStatusOrderByCreatedAtDesc(
            UUID communityId,
            CommunityJoinRequestStatus status,
            Pageable pageable
    );

    List<CommunityJoinRequestJpaEntity> findByCommunityIdAndStatusOrderByCreatedAtDesc(
            UUID communityId,
            CommunityJoinRequestStatus status
    );
}
