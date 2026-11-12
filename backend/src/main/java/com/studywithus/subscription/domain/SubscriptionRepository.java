package com.studywithus.subscription.domain;

import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository {
    Subscription save(Subscription subscription);
    Optional<Subscription> findById(UUID id);
    Optional<Subscription> findByUserId(UUID userId);
    Optional<Subscription> findByExternalId(String externalId);
    boolean existsByUserIdAndStatus(UUID userId, SubscriptionStatus status);
}
