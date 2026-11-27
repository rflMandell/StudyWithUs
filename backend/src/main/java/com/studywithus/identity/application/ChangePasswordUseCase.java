package com.studywithus.identity.application;

import com.studywithus.identity.domain.UserCredential;
import com.studywithus.identity.domain.UserRepository;
import com.studywithus.shared.application.UseCase;
import com.studywithus.shared.domain.DomainException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ChangePasswordUseCase implements UseCase<ChangePasswordCommand, Void> {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    @Override
    @Transactional
    public Void execute(ChangePasswordCommand command) {
        if (!command.newPassword().equals(command.confirmPassword())) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "PASSWORD_MISMATCH", "New password and confirmation do not match");
        }

        UserCredential credential = userRepository.findCredentialByUserId(command.userId())
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User credentials not found"));

        if (!passwordHasher.matches(command.currentPassword(), credential.getPasswordHash())) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "INVALID_CURRENT_PASSWORD", "Current password is incorrect");
        }

        if (passwordHasher.matches(command.newPassword(), credential.getPasswordHash())) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "SAME_PASSWORD", "New password must be different from current password");
        }

        credential.setPasswordHash(passwordHasher.hash(command.newPassword()));
        credential.setUpdatedAt(Instant.now());
        userRepository.saveCredential(credential);
        return null;
    }
}
