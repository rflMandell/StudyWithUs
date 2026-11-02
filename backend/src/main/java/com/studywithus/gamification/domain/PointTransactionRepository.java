package com.studywithus.gamification.domain;

import com.studywithus.shared.domain.PagedResult;

import java.util.Optional;
import java.util.UUID;

public interface PointTransactionRepository {
    PointTransaction save(PointTransaction transaction);
    Optional<PointTransaction> findById(UUID id);
    Optional<PointTransaction> findByCheckinIdAndType(UUID checkinId, PointTransactionType type);
    PagedResult<PointTransaction> findByUserId(UUID userId, int page, int size);
    long calculateTotalPointsByUser(UUID userId);
    long calculateTotalPointsByUserAndCommunity(UUID userId, UUID communityId);
}
