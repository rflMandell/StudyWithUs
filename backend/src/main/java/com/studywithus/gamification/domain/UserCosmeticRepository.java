package com.studywithus.gamification.domain;

import java.util.List;
import java.util.UUID;

public interface UserCosmeticRepository {
    UserCosmetic save(UserCosmetic cosmetic);
    boolean existsByUserIdAndCosmeticCode(UUID userId, String cosmeticCode);
    List<UserCosmetic> findByUserId(UUID userId);
}
