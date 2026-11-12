package com.studywithus.subscription.application;

import com.studywithus.community.domain.CommunityRepository;
import com.studywithus.shared.domain.DomainException;
import com.studywithus.subscription.domain.SubscriptionPlan;
import com.studywithus.subscription.domain.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlanLimitService {

    public static final int MAX_COMMUNITIES_FREE = 1;
    public static final int MAX_COMMUNITIES_PREMIUM = 1000;

    private final SubscriptionRepository subscriptionRepository;
    private final CommunityRepository communityRepository;

    @Transactional(readOnly = true)
    public boolean isPremium(UUID userId) {
        return subscriptionRepository.findByUserId(userId)
                .map(sub -> sub.isActive() && sub.getPlan() == SubscriptionPlan.PREMIUM)
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public int getCommunityCreationLimit(UUID userId) {
        return isPremium(userId) ? MAX_COMMUNITIES_PREMIUM : MAX_COMMUNITIES_FREE;
    }

    @Transactional(readOnly = true)
    public void assertCanCreateCommunity(UUID userId) {
        if (isPremium(userId)) {
            return;
        }

        long createdCount = communityRepository.countByOwnerId(userId);
        if (createdCount >= MAX_COMMUNITIES_FREE) {
            throw new DomainException(
                    HttpStatus.FORBIDDEN,
                    "PAYWALL_LIMIT_EXCEEDED",
                    "O plano gratuito permite criar no máximo " + MAX_COMMUNITIES_FREE + " comunidade. Faça upgrade para o plano Premium para criar comunidades ilimitadas."
            );
        }
    }
}
