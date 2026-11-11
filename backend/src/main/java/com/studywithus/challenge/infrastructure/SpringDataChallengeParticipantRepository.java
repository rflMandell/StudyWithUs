package com.studywithus.challenge.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataChallengeParticipantRepository extends JpaRepository<ChallengeParticipantJpaEntity, ChallengeParticipantId> {

    Optional<ChallengeParticipantJpaEntity> findByIdChallengeIdAndIdUserId(UUID challengeId, UUID userId);

    List<ChallengeParticipantJpaEntity> findByIdChallengeId(UUID challengeId);

    @Query("SELECT p FROM ChallengeParticipantJpaEntity p " +
            "JOIN ChallengeJpaEntity c ON p.id.challengeId = c.id " +
            "WHERE p.id.userId = :userId " +
            "AND c.communityId = :communityId " +
            "AND c.status = 'ACTIVE' " +
            "AND c.startAt <= :now AND c.endAt >= :now " +
            "AND p.completedAt IS NULL")
    List<ChallengeParticipantJpaEntity> findActiveParticipationsByUserAndCommunity(
            @Param("userId") UUID userId,
            @Param("communityId") UUID communityId,
            @Param("now") Instant now
    );
}
