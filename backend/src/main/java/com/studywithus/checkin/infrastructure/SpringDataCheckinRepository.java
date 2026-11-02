package com.studywithus.checkin.infrastructure;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataCheckinRepository extends JpaRepository<CheckinJpaEntity, UUID> {

    @Query("SELECT c FROM CheckinJpaEntity c WHERE c.id = :id AND c.deletedAt IS NULL")
    Optional<CheckinJpaEntity> findActiveById(@Param("id") UUID id);

    @Query("SELECT c FROM CheckinJpaEntity c WHERE c.communityId = :communityId AND c.deletedAt IS NULL ORDER BY c.createdAt DESC")
    Page<CheckinJpaEntity> findActiveByCommunityId(@Param("communityId") UUID communityId, Pageable pageable);

    @Query("SELECT c FROM CheckinJpaEntity c WHERE c.userId = :userId AND c.deletedAt IS NULL ORDER BY c.createdAt DESC")
    Page<CheckinJpaEntity> findActiveByUserId(@Param("userId") UUID userId, Pageable pageable);
}
