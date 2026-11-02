package com.studywithus.gamification.application;

import com.studywithus.gamification.domain.PointTransaction;
import com.studywithus.gamification.domain.PointTransactionRepository;
import com.studywithus.gamification.domain.PointsAwardedEvent;
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
public class AwardPointsUseCase implements UseCase<AwardPointsCommand, PointTransactionResult> {

    private final PointTransactionRepository pointTransactionRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public PointTransactionResult execute(AwardPointsCommand command) {
        User user = userRepository.findById(command.userId())
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));

        PointTransaction transaction = PointTransaction.create(
                command.userId(),
                command.communityId(),
                command.checkinId(),
                command.challengeId(),
                command.amount(),
                command.type()
        );

        PointTransaction saved = pointTransactionRepository.save(transaction);

        if (command.amount() > 0) {
            user.setLifetimePoints(user.getLifetimePoints() + command.amount());
            userRepository.save(user);
        }

        eventPublisher.publishEvent(new PointsAwardedEvent(
                saved.getId(),
                saved.getUserId(),
                saved.getCommunityId(),
                saved.getAmount(),
                saved.getType()
        ));

        return PointTransactionResult.fromDomain(saved);
    }
}
