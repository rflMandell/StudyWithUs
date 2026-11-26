package com.studywithus.checkin.domain;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface CheckinLikeRepository {
    boolean exists(UUID checkinId, UUID userId);
    void save(UUID checkinId, UUID userId);
    void delete(UUID checkinId, UUID userId);
    long countByCheckinId(UUID checkinId);
    Map<UUID, Long> countByCheckinIds(Collection<UUID> checkinIds);
    Set<UUID> findLikedCheckinIdsByUser(Collection<UUID> checkinIds, UUID userId);
}
