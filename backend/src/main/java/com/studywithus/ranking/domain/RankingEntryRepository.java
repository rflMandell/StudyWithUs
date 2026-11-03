package com.studywithus.ranking.domain;

import com.studywithus.shared.domain.PagedResult;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RankingEntryRepository {
    RankingEntry save(RankingEntry entry);
    List<RankingEntry> saveAll(List<RankingEntry> entries);
    Optional<RankingEntry> findByPeriodIdAndUserId(UUID rankingPeriodId, UUID userId);
    List<RankingEntry> findByPeriodIdOrderByPointsDesc(UUID rankingPeriodId);
    PagedResult<RankingEntry> findByPeriodIdOrderByPositionAsc(UUID rankingPeriodId, int page, int size);
}
