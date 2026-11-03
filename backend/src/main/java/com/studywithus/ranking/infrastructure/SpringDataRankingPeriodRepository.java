package com.studywithus.ranking.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataRankingPeriodRepository extends JpaRepository<RankingPeriodJpaEntity, UUID> {

    @Query("SELECT p FROM RankingPeriodJpaEntity p WHERE p.communityId = :communityId AND p.status = 'ACTIVE' ORDER BY p.createdAt DESC LIMIT 1")
    Optional<RankingPeriodJpaEntity> findActiveByCommunityId(@Param("communityId") UUID communityId);
}
