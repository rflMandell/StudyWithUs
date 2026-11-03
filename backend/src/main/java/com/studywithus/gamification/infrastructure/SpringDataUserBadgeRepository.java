package com.studywithus.gamification.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface SpringDataUserBadgeRepository extends JpaRepository<UserBadgeJpaEntity, UserBadgeId> {

    @Query("SELECT ub FROM UserBadgeJpaEntity ub JOIN FETCH ub.badge WHERE ub.id.userId = :userId ORDER BY ub.awardedAt DESC")
    List<UserBadgeJpaEntity> findByUserIdWithBadge(@Param("userId") UUID userId);
}
