package com.studywithus.subscription.infrastructure;

import com.studywithus.subscription.domain.Subscription;
import com.studywithus.subscription.domain.SubscriptionRepository;
import com.studywithus.subscription.domain.SubscriptionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SubscriptionRepositoryAdapter implements SubscriptionRepository {

    private final SpringDataSubscriptionRepository springDataSubscriptionRepository;

    @Override
    public Subscription save(Subscription subscription) {
        SubscriptionJpaEntity entity = SubscriptionJpaEntity.fromDomain(subscription);
        return springDataSubscriptionRepository.save(entity).toDomain();
    }

    @Override
    public Optional<Subscription> findById(UUID id) {
        return springDataSubscriptionRepository.findById(id).map(SubscriptionJpaEntity::toDomain);
    }

    @Override
    public Optional<Subscription> findByUserId(UUID userId) {
        return springDataSubscriptionRepository.findByUserId(userId).map(SubscriptionJpaEntity::toDomain);
    }

    @Override
    public Optional<Subscription> findByExternalId(String externalId) {
        return springDataSubscriptionRepository.findByExternalId(externalId).map(SubscriptionJpaEntity::toDomain);
    }

    @Override
    public boolean existsByUserIdAndStatus(UUID userId, SubscriptionStatus status) {
        return springDataSubscriptionRepository.existsByUserIdAndStatus(userId, status.name());
    }
}
