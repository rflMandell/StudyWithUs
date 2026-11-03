package com.studywithus.gamification.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataBadgeRepository extends JpaRepository<BadgeJpaEntity, UUID> {
}
