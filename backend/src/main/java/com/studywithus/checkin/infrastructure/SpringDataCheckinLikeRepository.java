package com.studywithus.checkin.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface SpringDataCheckinLikeRepository extends JpaRepository<CheckinLikeJpaEntity, CheckinLikeId> {

    @Query("SELECT COUNT(l) FROM CheckinLikeJpaEntity l WHERE l.id.checkinId = :checkinId")
    long countByCheckinId(@Param("checkinId") UUID checkinId);

    @Query("SELECT l.id.checkinId, COUNT(l) FROM CheckinLikeJpaEntity l WHERE l.id.checkinId IN :checkinIds GROUP BY l.id.checkinId")
    List<Object[]> countGroupedByCheckinIds(@Param("checkinIds") Collection<UUID> checkinIds);

    @Query("SELECT l.id.checkinId FROM CheckinLikeJpaEntity l WHERE l.id.checkinId IN :checkinIds AND l.id.userId = :userId")
    Set<UUID> findLikedCheckinIdsByUser(@Param("checkinIds") Collection<UUID> checkinIds, @Param("userId") UUID userId);
}
