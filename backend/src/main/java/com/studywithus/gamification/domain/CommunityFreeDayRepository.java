package com.studywithus.gamification.domain;

import java.util.List;
import java.util.UUID;

public interface CommunityFreeDayRepository {
    CommunityFreeDay save(CommunityFreeDay freeDay);
    List<CommunityFreeDay> findByCommunityId(UUID communityId);
    List<Integer> findDaysOfWeekByCommunityId(UUID communityId);
    boolean existsByCommunityIdAndDayOfWeek(UUID communityId, int dayOfWeek);
    void deleteByCommunityIdAndDayOfWeek(UUID communityId, int dayOfWeek);
}
