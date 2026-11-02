package com.studywithus.gamification.application;

import com.studywithus.gamification.domain.PointTransactionRepository;
import com.studywithus.identity.domain.User;
import com.studywithus.identity.domain.UserRepository;
import com.studywithus.shared.application.UseCase;
import com.studywithus.shared.domain.DomainException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetUserPointsSummaryUseCase implements UseCase<UUID, UserPointsSummaryResult> {

    private final PointTransactionRepository pointTransactionRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserPointsSummaryResult execute(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));

        long currentPoints = pointTransactionRepository.calculateTotalPointsByUser(userId);

        return new UserPointsSummaryResult(
                userId,
                currentPoints,
                user.getLifetimePoints()
        );
    }
}
