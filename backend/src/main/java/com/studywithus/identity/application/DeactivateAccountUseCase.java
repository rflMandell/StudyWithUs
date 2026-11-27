package com.studywithus.identity.application;

import com.studywithus.identity.domain.RefreshTokenRepository;
import com.studywithus.identity.domain.User;
import com.studywithus.identity.domain.UserRepository;
import com.studywithus.identity.domain.UserStatus;
import com.studywithus.shared.application.UseCase;
import com.studywithus.shared.domain.DomainException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeactivateAccountUseCase implements UseCase<UUID, Void> {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    @Transactional
    public Void execute(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));

        Instant now = Instant.now();
        user.setStatus(UserStatus.DEACTIVATED);
        user.setDeactivatedAt(now);
        user.setUpdatedAt(now);
        userRepository.save(user);

        refreshTokenRepository.revokeAllByUserId(userId);
        return null;
    }
}
