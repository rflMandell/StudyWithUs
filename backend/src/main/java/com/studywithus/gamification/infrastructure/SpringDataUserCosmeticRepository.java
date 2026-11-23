package com.studywithus.gamification.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataUserCosmeticRepository extends JpaRepository<UserCosmeticJpaEntity, UUID> {
    boolean existsByUserIdAndCosmeticCode(UUID userId, String cosmeticCode);
    List<UserCosmeticJpaEntity> findByUserIdOrderByUnlockedAtLevelAsc(UUID userId);
}
