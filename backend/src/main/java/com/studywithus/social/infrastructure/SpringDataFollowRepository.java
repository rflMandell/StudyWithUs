package com.studywithus.social.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface SpringDataFollowRepository extends JpaRepository<FollowJpaEntity, FollowId> {

    @Query("SELECT f.id.followingId FROM FollowJpaEntity f WHERE f.id.followerId = :followerId")
    List<UUID> findFollowingIdsByFollowerId(@Param("followerId") UUID followerId);
}
