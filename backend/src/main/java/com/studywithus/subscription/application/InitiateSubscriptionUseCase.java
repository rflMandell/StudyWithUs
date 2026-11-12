package com.studywithus.subscription.application;

import com.studywithus.identity.domain.User;
import com.studywithus.identity.domain.UserRepository;
import com.studywithus.shared.application.UseCase;
import com.studywithus.shared.domain.DomainException;
import com.studywithus.subscription.domain.CheckoutSession;
import com.studywithus.subscription.domain.PaymentProviderPort;
import com.studywithus.subscription.domain.Subscription;
import com.studywithus.subscription.domain.SubscriptionCreatedEvent;
import com.studywithus.subscription.domain.SubscriptionPlan;
import com.studywithus.subscription.domain.SubscriptionProvider;
import com.studywithus.subscription.domain.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InitiateSubscriptionUseCase implements UseCase<InitiateSubscriptionCommand, InitiateSubscriptionResult> {

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final PaymentProviderPort paymentProviderPort;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public InitiateSubscriptionResult execute(InitiateSubscriptionCommand command) {
        userRepository.findById(command.userId())
                .filter(User::isActive)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));

        SubscriptionPlan targetPlan = command.plan() != null ? command.plan() : SubscriptionPlan.PREMIUM;

        subscriptionRepository.findByUserId(command.userId()).ifPresent(existing -> {
            if (existing.isActive() && existing.getPlan() == targetPlan) {
                throw new DomainException(HttpStatus.CONFLICT, "ALREADY_SUBSCRIBED", "User already has an active subscription to this plan");
            }
        });

        CheckoutSession session = paymentProviderPort.createCheckoutSession(
                command.userId(),
                targetPlan,
                command.returnUrl()
        );

        Subscription subscription = subscriptionRepository.findByUserId(command.userId())
                .map(existing -> {
                    existing.setExternalId(session.sessionId());
                    existing.setPlan(targetPlan);
                    return existing;
                })
                .orElseGet(() -> Subscription.createPending(
                        command.userId(),
                        SubscriptionProvider.STRIPE,
                        session.sessionId(),
                        targetPlan
                ));

        Subscription saved = subscriptionRepository.save(subscription);

        eventPublisher.publishEvent(new SubscriptionCreatedEvent(
                saved.getId(),
                saved.getUserId(),
                saved.getPlan()
        ));

        return new InitiateSubscriptionResult(
                saved.getId(),
                session.sessionId(),
                session.checkoutUrl(),
                saved.getPlan()
        );
    }
}
