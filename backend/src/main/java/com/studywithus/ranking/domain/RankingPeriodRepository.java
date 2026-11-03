package com.studywithus.ranking.domain;

import java.util.Optional;
import java.util.UUID;

public interface RankingPeriodRepository {
    RankingPeriod save(RankingPeriod period);
    Optional<RankingPeriod> findActiveByCommunityId(UUID communityId);
    Optional<RankingPeriod> findById(UUID id);
}
