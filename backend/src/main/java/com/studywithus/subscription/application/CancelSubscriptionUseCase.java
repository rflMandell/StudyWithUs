package com.studywithus.subscription.application;

import com.studywithus.shared.domain.DomainException;
import com.studywithus.subscription.domain.Subscription;
import com.studywithus.subscription.domain.SubscriptionCanceledEvent;
import com.studywithus.subscription.domain.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CancelSubscriptionUseCase {

    private final SubscriptionRepository subscriptionRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public SubscriptionResult execute(UUID userId) {
        Subscription subscription = subscriptionRepository.findByUserId(userId)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "SUBSCRIPTION_NOT_FOUND", "No subscription found for user"));

        if (!subscription.isActive()) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "SUBSCRIPTION_NOT_ACTIVE", "Subscription is not active");
        }

        subscription.cancel();
        Subscription saved = subscriptionRepository.save(subscription);

        eventPublisher.publishEvent(new SubscriptionCanceledEvent(saved.getId(), saved.getUserId()));

        return SubscriptionResult.fromDomain(saved);
    }
}
