package com.studywithus.community.infrastructure;

import com.studywithus.community.domain.MemberStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataCommunityMemberRepository extends JpaRepository<CommunityMemberJpaEntity, CommunityMemberId> {

    @Query("SELECT cm FROM CommunityMemberJpaEntity cm WHERE cm.id.communityId = :communityId AND cm.id.userId = :userId")
    Optional<CommunityMemberJpaEntity> findByCommunityIdAndUserId(
            @Param("communityId") UUID communityId,
            @Param("userId") UUID userId
    );

    @Query("SELECT COUNT(cm) > 0 FROM CommunityMemberJpaEntity cm WHERE cm.id.communityId = :communityId AND cm.id.userId = :userId AND cm.status = 'ACTIVE'")
    boolean isUserActiveMember(@Param("communityId") UUID communityId, @Param("userId") UUID userId);

    @Query("SELECT COUNT(cm) FROM CommunityMemberJpaEntity cm WHERE cm.id.communityId = :communityId AND cm.status = 'ACTIVE'")
    long countActiveMembers(@Param("communityId") UUID communityId);

    @Query("SELECT cm FROM CommunityMemberJpaEntity cm WHERE cm.id.communityId = :communityId AND cm.status = :status ORDER BY cm.joinedAt ASC")
    Page<CommunityMemberJpaEntity> findByCommunityIdAndStatus(
            @Param("communityId") UUID communityId,
            @Param("status") MemberStatus status,
            Pageable pageable
    );

    @Query("SELECT cm.id.communityId FROM CommunityMemberJpaEntity cm WHERE cm.id.userId = :userId AND cm.status = 'ACTIVE'")
    java.util.List<UUID> findActiveCommunityIdsByUserId(@Param("userId") UUID userId);

    @Query("SELECT cm.id.userId FROM CommunityMemberJpaEntity cm WHERE cm.id.communityId = :communityId AND cm.status = 'ACTIVE'")
    java.util.List<UUID> findActiveUserIdsByCommunityId(@Param("communityId") UUID communityId);

    @Query("SELECT cm.id.communityId, COUNT(cm) FROM CommunityMemberJpaEntity cm WHERE cm.id.communityId IN :communityIds AND cm.status = 'ACTIVE' GROUP BY cm.id.communityId")
    List<Object[]> countActiveMembersByCommunityIds(@Param("communityIds") Collection<UUID> communityIds);

    @Query("SELECT cm FROM CommunityMemberJpaEntity cm WHERE cm.id.communityId IN :communityIds AND cm.id.userId = :userId AND cm.status = 'ACTIVE'")
    List<CommunityMemberJpaEntity> findActiveByCommunityIdsAndUserId(
            @Param("communityIds") Collection<UUID> communityIds,
            @Param("userId") UUID userId
    );
}
