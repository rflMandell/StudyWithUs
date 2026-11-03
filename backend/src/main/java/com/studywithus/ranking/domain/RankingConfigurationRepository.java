package com.studywithus.ranking.domain;

import java.util.Optional;
import java.util.UUID;

public interface RankingConfigurationRepository {
    RankingConfiguration save(RankingConfiguration config);
    Optional<RankingConfiguration> findByCommunityId(UUID communityId);
}
