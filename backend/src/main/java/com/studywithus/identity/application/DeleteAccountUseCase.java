package com.studywithus.identity.application;

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
public class DeleteAccountUseCase implements UseCase<UUID, Void> {

    private final UserRepository userRepository;
    private final AccountDeletionPort accountDeletionPort;

    @Override
    @Transactional
    public Void execute(UUID userId) {
        if (userRepository.findById(userId).isEmpty()) {
            throw new DomainException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found");
        }
        accountDeletionPort.deleteUserPermanently(userId);
        return null;
    }
}
