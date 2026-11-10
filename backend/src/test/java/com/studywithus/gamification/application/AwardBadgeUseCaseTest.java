package com.studywithus.gamification.application;

import com.studywithus.gamification.domain.Badge;
import com.studywithus.gamification.domain.BadgeGrantedEvent;
import com.studywithus.gamification.domain.BadgeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AwardBadgeUseCaseTest {

    @Mock
    private BadgeRepository badgeRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private AwardBadgeUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new AwardBadgeUseCase(badgeRepository, eventPublisher);
    }

    @Test
    @DisplayName("should award badge and publish BadgeGrantedEvent when user does not have badge")
    void shouldAwardBadgeSuccessfully() {
        UUID userId = UUID.randomUUID();
        UUID badgeId = UUID.randomUUID();
        Badge badge = Badge.builder()
                .id(badgeId)
                .name("7 Dias Seguidos")
                .description("Descrição")
                .createdAt(Instant.now())
                .build();

        when(badgeRepository.findById(badgeId)).thenReturn(Optional.of(badge));
        when(badgeRepository.hasBadge(userId, badgeId)).thenReturn(false);

        boolean awarded = useCase.execute(new AwardBadgeCommand(userId, badgeId));

        assertThat(awarded).isTrue();
        verify(badgeRepository).assignBadgeToUser(eq(userId), eq(badgeId), any(), any(), any());

        ArgumentCaptor<BadgeGrantedEvent> eventCaptor = ArgumentCaptor.forClass(BadgeGrantedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().userId()).isEqualTo(userId);
        assertThat(eventCaptor.getValue().badgeId()).isEqualTo(badgeId);
        assertThat(eventCaptor.getValue().badgeName()).isEqualTo("7 Dias Seguidos");
    }

    @Test
    @DisplayName("should skip assignment and be idempotent when user already has badge")
    void shouldBeIdempotentWhenUserAlreadyHasBadge() {
        UUID userId = UUID.randomUUID();
        UUID badgeId = UUID.randomUUID();
        Badge badge = Badge.builder().id(badgeId).name("7 Dias").build();

        when(badgeRepository.findById(badgeId)).thenReturn(Optional.of(badge));
        when(badgeRepository.hasBadge(userId, badgeId)).thenReturn(true);

        boolean awarded = useCase.execute(new AwardBadgeCommand(userId, badgeId));

        assertThat(awarded).isFalse();
        verify(badgeRepository, never()).assignBadgeToUser(any(), any(), any(), any(), any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("should return false when badge does not exist")
    void shouldReturnFalseWhenBadgeDoesNotExist() {
        UUID userId = UUID.randomUUID();
        UUID badgeId = UUID.randomUUID();

        when(badgeRepository.findById(badgeId)).thenReturn(Optional.empty());

        boolean awarded = useCase.execute(new AwardBadgeCommand(userId, badgeId));

        assertThat(awarded).isFalse();
        verify(badgeRepository, never()).assignBadgeToUser(any(), any(), any(), any(), any());
    }
}
