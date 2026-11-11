package com.studywithus.challenge.infrastructure;

import com.studywithus.challenge.domain.ChallengeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface SpringDataChallengeRepository extends JpaRepository<ChallengeJpaEntity, UUID> {

    List<ChallengeJpaEntity> findByCommunityIdOrderByCreatedAtDesc(UUID communityId);

    @Query("SELECT c FROM ChallengeJpaEntity c WHERE c.communityId = :communityId " +
            "AND c.status = :status AND c.startAt <= :now AND c.endAt >= :now " +
            "ORDER BY c.createdAt DESC")
    List<ChallengeJpaEntity> findActiveChallenges(
            @Param("communityId") UUID communityId,
            @Param("status") ChallengeStatus status,
            @Param("now") Instant now
    );
}
