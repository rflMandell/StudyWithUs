package com.studywithus.gamification.application;

import com.studywithus.gamification.domain.PointTransaction;
import com.studywithus.gamification.domain.PointTransactionRepository;
import com.studywithus.gamification.domain.PointsReversedEvent;
import com.studywithus.identity.domain.User;
import com.studywithus.identity.domain.UserRepository;
import com.studywithus.shared.application.UseCase;
import com.studywithus.shared.domain.DomainException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReversePointsUseCase implements UseCase<ReversePointsCommand, PointTransactionResult> {

    private final PointTransactionRepository pointTransactionRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public PointTransactionResult execute(ReversePointsCommand command) {
        PointTransaction original = pointTransactionRepository.findById(command.originalTransactionId())
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "TRANSACTION_NOT_FOUND", "Point transaction not found"));

        if (original.isReversed()) {
            throw new DomainException(HttpStatus.CONFLICT, "TRANSACTION_ALREADY_REVERSED", "Transaction has already been reversed");
        }

        int reversalAmount = -Math.abs(original.getAmount());
        PointTransaction reversal = PointTransaction.createReversal(original, reversalAmount);

        original.markReversed(reversal.getId());

        pointTransactionRepository.save(original);
        PointTransaction savedReversal = pointTransactionRepository.save(reversal);

        userRepository.findById(original.getUserId()).ifPresent(user -> {
            user.setLifetimePoints(Math.max(0, user.getLifetimePoints() - Math.abs(original.getAmount())));
            userRepository.save(user);
        });

        eventPublisher.publishEvent(new PointsReversedEvent(
                savedReversal.getId(),
                original.getId(),
                original.getUserId(),
                original.getCommunityId(),
                savedReversal.getAmount()
        ));

        return PointTransactionResult.fromDomain(savedReversal);
    }
}
