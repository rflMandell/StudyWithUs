package com.studywithus.challenge.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataChallengeRewardRepository extends JpaRepository<ChallengeRewardJpaEntity, UUID> {
    List<ChallengeRewardJpaEntity> findByChallengeId(UUID challengeId);
}
