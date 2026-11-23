package com.studywithus.gamification.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface SpringDataUserLevelRepository extends JpaRepository<UserLevelJpaEntity, UUID> {
    List<UserLevelJpaEntity> findByUserIdIn(Collection<UUID> userIds);
}
