package com.studywithus.identity.application;

import com.studywithus.identity.domain.RefreshToken;
import com.studywithus.identity.domain.RefreshTokenRepository;
import com.studywithus.identity.domain.User;
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
public class LoginUseCase implements UseCase<LoginCommand, AuthResult> {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordHasher passwordHasher;
    private final TokenServicePort tokenService;

    @Override
    @Transactional
    public AuthResult execute(LoginCommand command) {
        String identifier = command.usernameOrEmail().trim().toLowerCase();

        User user = userRepository.findByUsernameOrEmail(identifier)
                .orElseThrow(() -> new DomainException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid username/email or password"));

        if (!user.isActive()) {
            throw new DomainException(HttpStatus.FORBIDDEN, "USER_SUSPENDED", "User account is suspended or deleted");
        }

        UserCredential credential = userRepository.findCredentialByUserId(user.getId())
                .orElseThrow(() -> new DomainException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid username/email or password"));

        if (!passwordHasher.matches(command.password(), credential.getPasswordHash())) {
            throw new DomainException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid username/email or password");
        }

        Instant now = Instant.now();
        String accessToken = tokenService.generateAccessToken(user);
        String rawRefreshToken = tokenService.generateRefreshToken();
        String tokenHash = tokenService.hashToken(rawRefreshToken);
        Instant refreshTokenExpiresAt = now.plusSeconds(tokenService.getRefreshTokenExpirationSeconds());

        RefreshToken refreshToken = RefreshToken.builder()
                .userId(user.getId())
                .tokenHash(tokenHash)
                .expiresAt(refreshTokenExpiresAt)
                .createdAt(now)
                .deviceId(command.deviceId())
                .build();
        refreshTokenRepository.save(refreshToken);

        return AuthResult.bearer(
                accessToken,
                rawRefreshToken,
                tokenService.getAccessTokenExpirationSeconds(),
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getName()
        );
    }
}
