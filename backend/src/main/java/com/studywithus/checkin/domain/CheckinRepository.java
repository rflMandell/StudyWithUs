package com.studywithus.checkin.domain;

import com.studywithus.shared.domain.PagedResult;

import java.util.Optional;
import java.util.UUID;

public interface CheckinRepository {
    Checkin save(Checkin checkin);
    Optional<Checkin> findById(UUID id);
    PagedResult<Checkin> findByCommunityIdActive(UUID communityId, int page, int size);
    PagedResult<Checkin> findByUserIdActive(UUID userId, int page, int size);
}
