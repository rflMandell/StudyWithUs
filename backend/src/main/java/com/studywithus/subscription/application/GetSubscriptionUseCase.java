package com.studywithus.subscription.application;

import com.studywithus.subscription.domain.Subscription;
import com.studywithus.subscription.domain.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetSubscriptionUseCase {

    private final SubscriptionRepository subscriptionRepository;

    @Transactional
    public SubscriptionResult execute(UUID userId) {
        return subscriptionRepository.findByUserId(userId)
                .map(sub -> {
                    if (sub.getStatus() == com.studywithus.subscription.domain.SubscriptionStatus.ACTIVE && !sub.isActive()) {
                        sub.expire();
                        subscriptionRepository.save(sub);
                    }
                    return SubscriptionResult.fromDomain(sub);
                })
                .orElseGet(() -> SubscriptionResult.free(userId));
    }
}
