package com.studywithus.notification.application;

import com.studywithus.notification.domain.DeviceToken;
import com.studywithus.notification.domain.DeviceTokenRepository;
import com.studywithus.shared.application.UseCase;
import com.studywithus.shared.domain.DomainException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RegisterDeviceTokenUseCase implements UseCase<RegisterDeviceTokenCommand, Void> {

    private final DeviceTokenRepository repository;

    @Override
    @Transactional
    public Void execute(RegisterDeviceTokenCommand command) {
        if (command.token() == null || command.token().isBlank()) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "INVALID_TOKEN", "O token do dispositivo é obrigatório");
        }

        Optional<DeviceToken> existingOpt = repository.findByUserIdAndToken(command.userId(), command.token());
        if (existingOpt.isPresent()) {
            DeviceToken existing = existingOpt.get();
            existing.updateLastSeen();
            repository.save(existing);
        } else {
            DeviceToken newToken = DeviceToken.create(command.userId(), command.token(), command.platform());
            repository.save(newToken);
        }

        return null;
    }
}
