package com.studywithus.subscription.application;

import com.studywithus.identity.domain.User;
import com.studywithus.identity.domain.UserRepository;
import com.studywithus.shared.domain.DomainException;
import com.studywithus.subscription.domain.CheckoutSession;
import com.studywithus.subscription.domain.PaymentProviderPort;
import com.studywithus.subscription.domain.Subscription;
import com.studywithus.subscription.domain.SubscriptionCreatedEvent;
import com.studywithus.subscription.domain.SubscriptionPlan;
import com.studywithus.subscription.domain.SubscriptionRepository;
import com.studywithus.subscription.domain.SubscriptionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InitiateSubscriptionUseCaseTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PaymentProviderPort paymentProviderPort;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private InitiateSubscriptionUseCase useCase;

    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = mock(User.class);
    }

    @Test
    @DisplayName("Should successfully initiate subscription checkout")
    void shouldInitiateSubscription() {
        when(user.isActive()).thenReturn(true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(subscriptionRepository.findByUserId(userId)).thenReturn(Optional.empty());

        CheckoutSession session = new CheckoutSession("cs_123", "https://checkout.studywithus.com/cs_123");
        when(paymentProviderPort.createCheckoutSession(userId, SubscriptionPlan.PREMIUM, "https://app.studywithus.com/success"))
                .thenReturn(session);

        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> inv.getArgument(0));

        InitiateSubscriptionCommand command = new InitiateSubscriptionCommand(
                userId,
                SubscriptionPlan.PREMIUM,
                "https://app.studywithus.com/success"
        );

        InitiateSubscriptionResult result = useCase.execute(command);

        assertThat(result).isNotNull();
        assertThat(result.sessionId()).isEqualTo("cs_123");
        assertThat(result.checkoutUrl()).contains("cs_123");
        assertThat(result.plan()).isEqualTo(SubscriptionPlan.PREMIUM);

        verify(subscriptionRepository).save(any(Subscription.class));
        verify(eventPublisher).publishEvent(any(SubscriptionCreatedEvent.class));
    }

    @Test
    @DisplayName("Should throw CONFLICT when user already has an active subscription")
    void shouldThrowWhenAlreadySubscribed() {
        when(user.isActive()).thenReturn(true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        Subscription activeSub = mock(Subscription.class);
        when(activeSub.isActive()).thenReturn(true);
        when(activeSub.getPlan()).thenReturn(SubscriptionPlan.PREMIUM);
        when(subscriptionRepository.findByUserId(userId)).thenReturn(Optional.of(activeSub));

        InitiateSubscriptionCommand command = new InitiateSubscriptionCommand(
                userId,
                SubscriptionPlan.PREMIUM,
                null
        );

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }
}
