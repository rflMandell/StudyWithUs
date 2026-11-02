package com.studywithus.gamification.application;

import com.studywithus.gamification.domain.PointTransaction;
import com.studywithus.gamification.domain.PointTransactionRepository;
import com.studywithus.gamification.domain.PointTransactionType;
import com.studywithus.gamification.domain.PointsAwardedEvent;
import com.studywithus.identity.domain.User;
import com.studywithus.identity.domain.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AwardPointsUseCaseTest {

    @Mock
    private PointTransactionRepository pointTransactionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AwardPointsUseCase awardPointsUseCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID communityId = UUID.randomUUID();
    private final UUID checkinId = UUID.randomUUID();

    @Test
    @DisplayName("Award points saves transaction, increments user lifetimePoints, and publishes event")
    void awardPointsSuccessfully() {
        User user = User.builder()
                .id(userId)
                .lifetimePoints(50)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(pointTransactionRepository.save(any(PointTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AwardPointsCommand command = new AwardPointsCommand(
                userId,
                communityId,
                checkinId,
                null,
                10,
                PointTransactionType.CHECKIN
        );

        PointTransactionResult result = awardPointsUseCase.execute(command);

        assertThat(result.amount()).isEqualTo(10);
        assertThat(result.type()).isEqualTo(PointTransactionType.CHECKIN);
        assertThat(user.getLifetimePoints()).isEqualTo(60);
        verify(userRepository).save(user);
        verify(pointTransactionRepository).save(any(PointTransaction.class));
        verify(eventPublisher).publishEvent(any(PointsAwardedEvent.class));
    }
}
