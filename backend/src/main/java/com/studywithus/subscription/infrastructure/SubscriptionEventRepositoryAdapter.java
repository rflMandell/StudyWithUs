package com.studywithus.subscription.infrastructure;

import com.studywithus.subscription.domain.SubscriptionEvent;
import com.studywithus.subscription.domain.SubscriptionEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SubscriptionEventRepositoryAdapter implements SubscriptionEventRepository {

    private final SpringDataSubscriptionEventRepository springDataSubscriptionEventRepository;

    @Override
    public SubscriptionEvent save(SubscriptionEvent event) {
        SubscriptionEventJpaEntity entity = SubscriptionEventJpaEntity.fromDomain(event);
        return springDataSubscriptionEventRepository.save(entity).toDomain();
    }

    @Override
    public boolean existsByProviderEventId(String providerEventId) {
        return springDataSubscriptionEventRepository.existsByProviderEventId(providerEventId);
    }

    @Override
    public Optional<SubscriptionEvent> findByProviderEventId(String providerEventId) {
        return springDataSubscriptionEventRepository.findByProviderEventId(providerEventId).map(SubscriptionEventJpaEntity::toDomain);
    }
}
