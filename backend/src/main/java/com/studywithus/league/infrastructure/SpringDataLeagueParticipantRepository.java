package com.studywithus.league.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataLeagueParticipantRepository extends JpaRepository<LeagueParticipantJpaEntity, UUID> {
    Optional<LeagueParticipantJpaEntity> findBySeasonIdAndUserId(UUID seasonId, UUID userId);
    List<LeagueParticipantJpaEntity> findByPoolIdOrderByWeeklyScoreDescJoinedAtAsc(UUID poolId);
    List<LeagueParticipantJpaEntity> findBySeasonId(UUID seasonId);
}
