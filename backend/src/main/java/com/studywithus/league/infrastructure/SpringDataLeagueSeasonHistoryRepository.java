package com.studywithus.league.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataLeagueSeasonHistoryRepository extends JpaRepository<LeagueSeasonHistoryJpaEntity, UUID> {
    List<LeagueSeasonHistoryJpaEntity> findByUserIdOrderByRecordedAtDesc(UUID userId);
}
