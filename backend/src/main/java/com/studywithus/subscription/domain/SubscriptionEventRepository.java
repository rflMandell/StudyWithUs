package com.studywithus.subscription.domain;

import java.util.Optional;

public interface SubscriptionEventRepository {
    SubscriptionEvent save(SubscriptionEvent event);
    boolean existsByProviderEventId(String providerEventId);
    Optional<SubscriptionEvent> findByProviderEventId(String providerEventId);
}
