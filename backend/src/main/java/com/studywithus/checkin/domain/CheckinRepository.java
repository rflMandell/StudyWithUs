package com.studywithus.checkin.domain;

import com.studywithus.shared.domain.PagedResult;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CheckinRepository {
    Checkin save(Checkin checkin);
    Optional<Checkin> findById(UUID id);
    PagedResult<Checkin> findByCommunityIdActive(UUID communityId, int page, int size);
    PagedResult<Checkin> findByUserIdActive(UUID userId, int page, int size);
    long countActiveByUserId(UUID userId);
    List<Checkin> findFeedCheckins(Collection<UUID> communityIds, Collection<UUID> followingIds, Instant cursor, int limit);
    boolean existsActiveByUserIdBetween(UUID userId, Instant startInclusive, Instant endExclusive);
    boolean existsActiveByUserIdAndCommunityIdBetween(UUID userId, UUID communityId, Instant startInclusive, Instant endExclusive);
    List<Instant> findActiveCreatedAtByUserId(UUID userId);
    List<Instant> findActiveCreatedAtByUserIdAndCommunityId(UUID userId, UUID communityId);

    default List<Checkin> findFeedCheckins(Collection<UUID> communityIds, Collection<UUID> followingIds, Instant cursorTime, UUID cursorId, int limit) {
        return findFeedCheckins(communityIds, followingIds, cursorTime, limit);
    }

    default List<Checkin> findAllActiveByIds(Collection<UUID> ids) {
        return List.of();
    }

    default Optional<Checkin> findByIdIncludingDeleted(UUID id) {
        return findById(id);
    }
}
