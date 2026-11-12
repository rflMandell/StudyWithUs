package com.studywithus.subscription.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataSubscriptionRepository extends JpaRepository<SubscriptionJpaEntity, UUID> {
    Optional<SubscriptionJpaEntity> findByUserId(UUID userId);
    Optional<SubscriptionJpaEntity> findByExternalId(String externalId);
    boolean existsByUserIdAndStatus(UUID userId, String status);
}
