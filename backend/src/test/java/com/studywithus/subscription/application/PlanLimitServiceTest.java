package com.studywithus.subscription.application;

import com.studywithus.community.domain.CommunityRepository;
import com.studywithus.shared.domain.DomainException;
import com.studywithus.subscription.domain.Subscription;
import com.studywithus.subscription.domain.SubscriptionPlan;
import com.studywithus.subscription.domain.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanLimitServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private CommunityRepository communityRepository;

    @InjectMocks
    private PlanLimitService planLimitService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Free user with 0 communities should be allowed to create community")
    void freeUserCanCreateFirstCommunity() {
        when(subscriptionRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(communityRepository.countByOwnerId(userId)).thenReturn(0L);

        assertThatCode(() -> planLimitService.assertCanCreateCommunity(userId))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Free user with 1 community should be blocked from creating second community")
    void freeUserBlockedFromCreatingSecondCommunity() {
        when(subscriptionRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(communityRepository.countByOwnerId(userId)).thenReturn(1L);

        assertThatThrownBy(() -> planLimitService.assertCanCreateCommunity(userId))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> {
                    DomainException dex = (DomainException) ex;
                    assertThat(dex.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(dex.getCode()).isEqualTo("PAYWALL_LIMIT_EXCEEDED");
                });
    }

    @Test
    @DisplayName("Premium user can create multiple communities")
    void premiumUserCanCreateMultipleCommunities() {
        Subscription premiumSub = mock(Subscription.class);
        when(premiumSub.isActive()).thenReturn(true);
        when(premiumSub.getPlan()).thenReturn(SubscriptionPlan.PREMIUM);

        when(subscriptionRepository.findByUserId(userId)).thenReturn(Optional.of(premiumSub));

        assertThatCode(() -> planLimitService.assertCanCreateCommunity(userId))
                .doesNotThrowAnyException();
    }
}
