package com.studywithus.subscription.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataSubscriptionEventRepository extends JpaRepository<SubscriptionEventJpaEntity, UUID> {
    boolean existsByProviderEventId(String providerEventId);
    Optional<SubscriptionEventJpaEntity> findByProviderEventId(String providerEventId);
}
