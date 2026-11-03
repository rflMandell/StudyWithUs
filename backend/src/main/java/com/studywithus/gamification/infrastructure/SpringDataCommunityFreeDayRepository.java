package com.studywithus.gamification.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface SpringDataCommunityFreeDayRepository extends JpaRepository<CommunityFreeDayJpaEntity, UUID> {

    List<CommunityFreeDayJpaEntity> findByCommunityIdOrderByDayOfWeekAsc(UUID communityId);

    @Query("SELECT cfd.dayOfWeek FROM CommunityFreeDayJpaEntity cfd WHERE cfd.communityId = :communityId ORDER BY cfd.dayOfWeek ASC")
    List<Integer> findDaysOfWeekByCommunityId(@Param("communityId") UUID communityId);

    boolean existsByCommunityIdAndDayOfWeek(UUID communityId, int dayOfWeek);

    void deleteByCommunityIdAndDayOfWeek(UUID communityId, int dayOfWeek);
}
