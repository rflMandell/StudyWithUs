package com.studywithus.identity.application;

import com.studywithus.identity.domain.User;
import com.studywithus.identity.domain.UserRepository;
import com.studywithus.shared.application.UseCase;
import com.studywithus.shared.domain.DomainException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class UpdateAccountPrivacyUseCase implements UseCase<UpdateAccountPrivacyCommand, UserProfileResult> {

    private static final Duration PRIVACY_CHANGE_COOLDOWN = Duration.ofMinutes(10);

    private final UserRepository userRepository;
    private final GetUserProfileUseCase getUserProfileUseCase;

    @Override
    @Transactional
    public UserProfileResult execute(UpdateAccountPrivacyCommand command) {
        User user = userRepository.findById(command.userId())
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));

        if (user.isPrivate() == command.isPrivate()) {
            return getUserProfileUseCase.execute(command.userId());
        }

        Instant now = Instant.now();
        if (user.getPrivacyUpdatedAt() != null) {
            Instant nextAllowed = user.getPrivacyUpdatedAt().plus(PRIVACY_CHANGE_COOLDOWN);
            if (now.isBefore(nextAllowed)) {
                throw new DomainException(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "PRIVACY_RATE_LIMIT_EXCEEDED",
                        "Account privacy can only be changed once every 10 minutes"
                );
            }
        }

        user.setPrivate(command.isPrivate());
        user.setPrivacyUpdatedAt(now);
        user.setUpdatedAt(now);
        userRepository.save(user);

        return getUserProfileUseCase.execute(command.userId());
    }
}
