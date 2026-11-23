package com.studywithus.gamification.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserLevelRepository {
    UserLevel save(UserLevel userLevel);
    Optional<UserLevel> findByUserId(UUID userId);
    List<UserLevel> findAllByUserIds(Collection<UUID> userIds);
}
