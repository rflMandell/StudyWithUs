package com.studywithus.league.infrastructure;

import com.studywithus.league.domain.LeagueSeasonStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataLeagueSeasonRepository extends JpaRepository<LeagueSeasonJpaEntity, UUID> {
    Optional<LeagueSeasonJpaEntity> findFirstByStatusOrderByCreatedAtDesc(LeagueSeasonStatus status);
    Optional<LeagueSeasonJpaEntity> findByWeekKey(String weekKey);
}
