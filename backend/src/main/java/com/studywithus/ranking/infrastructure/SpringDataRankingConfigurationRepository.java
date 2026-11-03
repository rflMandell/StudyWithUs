package com.studywithus.ranking.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataRankingConfigurationRepository extends JpaRepository<RankingConfigurationJpaEntity, UUID> {
    Optional<RankingConfigurationJpaEntity> findByCommunityId(UUID communityId);
}
