package com.studywithus.subscription.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studywithus.shared.domain.DomainException;
import com.studywithus.subscription.domain.PaymentProviderPort;
import com.studywithus.subscription.domain.Subscription;
import com.studywithus.subscription.domain.SubscriptionActivatedEvent;
import com.studywithus.subscription.domain.SubscriptionCanceledEvent;
import com.studywithus.subscription.domain.SubscriptionEvent;
import com.studywithus.subscription.domain.SubscriptionEventRepository;
import com.studywithus.subscription.domain.SubscriptionPlan;
import com.studywithus.subscription.domain.SubscriptionProvider;
import com.studywithus.subscription.domain.SubscriptionRepository;
import com.studywithus.subscription.domain.SubscriptionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessWebhookUseCaseTest {

    @Mock
    private PaymentProviderPort paymentProviderPort;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private SubscriptionEventRepository subscriptionEventRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ProcessWebhookUseCase useCase;

    private UUID userId;
    private String sessionId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        sessionId = "cs_test_123";
    }

    @Test
    @DisplayName("Should activate subscription on checkout.session.completed event")
    void shouldActivateSubscriptionOnWebhook() {
        String payload = """
                {
                  "id": "evt_001",
                  "type": "checkout.session.completed",
                  "data": {
                    "object": {
                      "id": "cs_test_123"
                    }
                  }
                }
                """;

        when(paymentProviderPort.verifyWebhookSignature(payload, "valid-sig")).thenReturn(true);
        when(subscriptionEventRepository.existsByProviderEventId("evt_001")).thenReturn(false);

        Subscription pendingSub = Subscription.createPending(
                userId,
                SubscriptionProvider.STRIPE,
                sessionId,
                SubscriptionPlan.PREMIUM
        );

        when(subscriptionRepository.findByExternalId(sessionId)).thenReturn(Optional.of(pendingSub));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> inv.getArgument(0));

        useCase.execute(new ProcessWebhookCommand("valid-sig", payload));

        assertThat(pendingSub.isActive()).isTrue();
        assertThat(pendingSub.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);

        verify(subscriptionEventRepository).save(any(SubscriptionEvent.class));
        verify(subscriptionRepository).save(pendingSub);
        verify(eventPublisher).publishEvent(any(SubscriptionActivatedEvent.class));
    }

    @Test
    @DisplayName("Should ignore duplicate webhook events (idempotency)")
    void shouldIgnoreDuplicateEvent() {
        String payload = """
                {
                  "id": "evt_duplicate",
                  "type": "checkout.session.completed"
                }
                """;

        when(paymentProviderPort.verifyWebhookSignature(payload, "valid-sig")).thenReturn(true);
        when(subscriptionEventRepository.existsByProviderEventId("evt_duplicate")).thenReturn(true);

        useCase.execute(new ProcessWebhookCommand("valid-sig", payload));

        verify(subscriptionEventRepository, never()).save(any(SubscriptionEvent.class));
        verify(subscriptionRepository, never()).save(any(Subscription.class));
    }

    @Test
    @DisplayName("Should throw UNAUTHORIZED when signature verification fails")
    void shouldThrowWhenInvalidSignature() {
        String payload = "{}";
        when(paymentProviderPort.verifyWebhookSignature(payload, "invalid-sig")).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(new ProcessWebhookCommand("invalid-sig", payload)))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }
}
