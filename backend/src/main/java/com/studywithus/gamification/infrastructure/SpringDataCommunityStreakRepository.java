package com.studywithus.gamification.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataCommunityStreakRepository extends JpaRepository<CommunityStreakJpaEntity, CommunityStreakId> {
}
