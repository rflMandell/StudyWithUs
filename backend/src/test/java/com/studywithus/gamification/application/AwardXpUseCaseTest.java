package com.studywithus.gamification.application;

import com.studywithus.gamification.domain.UserCosmetic;
import com.studywithus.gamification.domain.UserCosmeticRepository;
import com.studywithus.gamification.domain.UserLevel;
import com.studywithus.gamification.domain.UserLevelRepository;
import com.studywithus.gamification.domain.UserLevelUpEvent;
import com.studywithus.gamification.domain.XpSourceType;
import com.studywithus.gamification.domain.XpTransaction;
import com.studywithus.gamification.domain.XpTransactionRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AwardXpUseCaseTest {

    @Mock
    private UserLevelRepository userLevelRepository;

    @Mock
    private XpTransactionRepository xpTransactionRepository;

    @Mock
    private UserCosmeticRepository userCosmeticRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private SimpleMeterRegistry meterRegistry;
    private AwardXpUseCase useCase;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        useCase = new AwardXpUseCase(
                userLevelRepository,
                xpTransactionRepository,
                userCosmeticRepository,
                eventPublisher,
                meterRegistry
        );
    }

    @Test
    void shouldLevelUpUserUnlockCosmeticsAndPublishEvent() {
        UUID userId = UUID.randomUUID();
        UserLevel current = UserLevel.initial(userId);
        current.applyXp(50L); // 50 / 100 at Level 1

        when(userLevelRepository.findByUserId(userId)).thenReturn(Optional.of(current));
        when(xpTransactionRepository.existsByUserIdAndSourceTypeAndSourceId(userId, XpSourceType.CHECKIN, "chk-1"))
                .thenReturn(false);
        when(xpTransactionRepository.save(any(XpTransaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userLevelRepository.save(any(UserLevel.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userCosmeticRepository.existsByUserIdAndCosmeticCode(any(), any())).thenReturn(false);
        when(userCosmeticRepository.save(any(UserCosmetic.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userCosmeticRepository.findByUserId(userId)).thenReturn(List.of());

        UserLevelResult result = useCase.execute(new AwardXpCommand(userId, XpSourceType.CHECKIN, "chk-1", 50L));

        assertThat(result.level()).isEqualTo(2);
        assertThat(result.totalXp()).isEqualTo(100L);
        assertThat(result.activeTitle()).isEqualTo("Estudante Dedicado");
        assertThat(result.activeAvatarBorder()).isEqualTo("BORDER_BRONZE");

        ArgumentCaptor<UserLevelUpEvent> eventCaptor = ArgumentCaptor.forClass(UserLevelUpEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().previousLevel()).isEqualTo(1);
        assertThat(eventCaptor.getValue().newLevel()).isEqualTo(2);
        assertThat(meterRegistry.counter("studywithus_level_ups_total").count()).isEqualTo(1.0);
        assertThat(meterRegistry.counter("studywithus_xp_awarded_total").count()).isEqualTo(50.0);
    }

    @Test
    void shouldBeIdempotentForDuplicateSource() {
        UUID userId = UUID.randomUUID();
        UserLevel current = UserLevel.initial(userId);

        when(userLevelRepository.findByUserId(userId)).thenReturn(Optional.of(current));
        when(xpTransactionRepository.existsByUserIdAndSourceTypeAndSourceId(userId, XpSourceType.CHECKIN, "chk-dup"))
                .thenReturn(true);
        when(userCosmeticRepository.findByUserId(userId)).thenReturn(List.of());

        UserLevelResult result = useCase.execute(new AwardXpCommand(userId, XpSourceType.CHECKIN, "chk-dup", 50L));

        assertThat(result.totalXp()).isEqualTo(0L);
        verify(xpTransactionRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }
}
