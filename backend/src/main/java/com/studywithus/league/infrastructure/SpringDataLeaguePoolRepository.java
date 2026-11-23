package com.studywithus.league.infrastructure;

import com.studywithus.league.domain.LeagueDivision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataLeaguePoolRepository extends JpaRepository<LeaguePoolJpaEntity, UUID> {

    Optional<LeaguePoolJpaEntity> findFirstBySeasonIdAndDivisionAndParticipantsCountLessThanOrderByPoolNumberAsc(
            UUID seasonId,
            LeagueDivision division,
            int maxParticipants
    );

    @Query("SELECT COALESCE(MAX(p.poolNumber), 0) FROM LeaguePoolJpaEntity p WHERE p.seasonId = :seasonId AND p.division = :division")
    int findMaxPoolNumber(@Param("seasonId") UUID seasonId, @Param("division") LeagueDivision division);

    List<LeaguePoolJpaEntity> findBySeasonIdOrderByDivisionAscPoolNumberAsc(UUID seasonId);
}
