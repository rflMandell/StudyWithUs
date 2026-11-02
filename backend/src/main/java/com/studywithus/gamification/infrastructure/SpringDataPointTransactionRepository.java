package com.studywithus.gamification.infrastructure;

import com.studywithus.gamification.domain.PointTransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataPointTransactionRepository extends JpaRepository<PointTransactionJpaEntity, UUID> {

    Page<PointTransactionJpaEntity> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Optional<PointTransactionJpaEntity> findByCheckinIdAndType(UUID checkinId, PointTransactionType type);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM PointTransactionJpaEntity t WHERE t.userId = :userId")
    long sumAmountByUserId(@Param("userId") UUID userId);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM PointTransactionJpaEntity t WHERE t.userId = :userId AND t.communityId = :communityId")
    long sumAmountByUserIdAndCommunityId(@Param("userId") UUID userId, @Param("communityId") UUID communityId);
}
