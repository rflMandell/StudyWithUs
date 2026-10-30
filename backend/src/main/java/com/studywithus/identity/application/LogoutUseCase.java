package com.studywithus.identity.application;

import com.studywithus.identity.domain.RefreshTokenRepository;
import com.studywithus.shared.application.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LogoutUseCase implements UseCase<LogoutCommand, Void> {

    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenServicePort tokenService;

    @Override
    @Transactional
    public Void execute(LogoutCommand command) {
        if (command.refreshToken() != null && !command.refreshToken().isBlank()) {
            String tokenHash = tokenService.hashToken(command.refreshToken());
            refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
                token.revoke();
                refreshTokenRepository.save(token);
            });
        }

        return null;
    }
}
