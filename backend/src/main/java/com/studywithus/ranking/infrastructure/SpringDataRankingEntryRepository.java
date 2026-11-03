package com.studywithus.ranking.infrastructure;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataRankingEntryRepository extends JpaRepository<RankingEntryJpaEntity, RankingEntryId> {

    Page<RankingEntryJpaEntity> findByRankingPeriodIdOrderByPositionAsc(UUID rankingPeriodId, Pageable pageable);

    List<RankingEntryJpaEntity> findByRankingPeriodIdOrderByPointsDescUpdatedAtAsc(UUID rankingPeriodId);

    Optional<RankingEntryJpaEntity> findByRankingPeriodIdAndUserId(UUID rankingPeriodId, UUID userId);
}
