package com.studywithus.gamification.application;

import com.studywithus.gamification.domain.PointTransaction;
import com.studywithus.gamification.domain.PointTransactionRepository;
import com.studywithus.gamification.domain.PointTransactionType;
import com.studywithus.gamification.domain.PointsReversedEvent;
import com.studywithus.identity.domain.User;
import com.studywithus.identity.domain.UserRepository;
import com.studywithus.shared.domain.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReversePointsUseCaseTest {

    @Mock
    private PointTransactionRepository pointTransactionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ReversePointsUseCase reversePointsUseCase;

    private final UUID transactionId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID communityId = UUID.randomUUID();

    @Test
    @DisplayName("Reverse points creates negative transaction, marks original reversed and updates user")
    void reversePointsSuccessfully() {
        PointTransaction original = PointTransaction.builder()
                .id(transactionId)
                .userId(userId)
                .communityId(communityId)
                .amount(10)
                .type(PointTransactionType.CHECKIN)
                .createdAt(Instant.now())
                .build();

        User user = User.builder()
                .id(userId)
                .lifetimePoints(60)
                .build();

        when(pointTransactionRepository.findById(transactionId)).thenReturn(Optional.of(original));
        when(pointTransactionRepository.save(any(PointTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        PointTransactionResult result = reversePointsUseCase.execute(new ReversePointsCommand(transactionId));

        assertThat(result.amount()).isEqualTo(-10);
        assertThat(result.type()).isEqualTo(PointTransactionType.REVERSAL);
        assertThat(original.isReversed()).isTrue();
        assertThat(original.getReversalId()).isEqualTo(result.id());
        assertThat(user.getLifetimePoints()).isEqualTo(50);

        verify(pointTransactionRepository, times(2)).save(any(PointTransaction.class));
        verify(userRepository).save(user);
        verify(eventPublisher).publishEvent(any(PointsReversedEvent.class));
    }

    @Test
    @DisplayName("Attempting to reverse an already-reversed transaction throws 409 TRANSACTION_ALREADY_REVERSED")
    void cannotReverseAlreadyReversed() {
        PointTransaction alreadyReversed = PointTransaction.builder()
                .id(transactionId)
                .userId(userId)
                .amount(10)
                .reversedAt(Instant.now())
                .reversalId(UUID.randomUUID())
                .build();

        when(pointTransactionRepository.findById(transactionId)).thenReturn(Optional.of(alreadyReversed));

        assertThatThrownBy(() -> reversePointsUseCase.execute(new ReversePointsCommand(transactionId)))
                .isInstanceOf(DomainException.class)
                .satisfies(e -> {
                    DomainException de = (DomainException) e;
                    assertThat(de.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(de.getCode()).isEqualTo("TRANSACTION_ALREADY_REVERSED");
                });
    }
}
