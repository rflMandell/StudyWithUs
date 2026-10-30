package com.studywithus.identity.application;

import com.studywithus.identity.domain.RefreshToken;
import com.studywithus.identity.domain.RefreshTokenRepository;
import com.studywithus.identity.domain.User;
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
public class RefreshTokenUseCase implements UseCase<RefreshTokenCommand, AuthResult> {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenServicePort tokenService;

    @Override
    @Transactional
    public AuthResult execute(RefreshTokenCommand command) {
        if (command.refreshToken() == null || command.refreshToken().isBlank()) {
            throw new DomainException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "Refresh token is required");
        }

        String tokenHash = tokenService.hashToken(command.refreshToken());
        RefreshToken existingToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new DomainException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "Invalid refresh token"));

        if (!existingToken.isValid()) {
            throw new DomainException(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_EXPIRED_OR_REVOKED", "Refresh token has expired or been revoked");
        }

        User user = userRepository.findById(existingToken.getUserId())
                .orElseThrow(() -> new DomainException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND", "User not found"));

        if (!user.isActive()) {
            throw new DomainException(HttpStatus.FORBIDDEN, "USER_SUSPENDED", "User account is suspended or deleted");
        }

        // Token Rotation (RB-07): revoke current token
        existingToken.revoke();
        refreshTokenRepository.save(existingToken);

        // Issue new token pair
        Instant now = Instant.now();
        String newAccessToken = tokenService.generateAccessToken(user);
        String newRawRefreshToken = tokenService.generateRefreshToken();
        String newTokenHash = tokenService.hashToken(newRawRefreshToken);
        Instant newExpiresAt = now.plusSeconds(tokenService.getRefreshTokenExpirationSeconds());

        RefreshToken newRefreshToken = RefreshToken.builder()
                .userId(user.getId())
                .tokenHash(newTokenHash)
                .expiresAt(newExpiresAt)
                .createdAt(now)
                .deviceId(command.deviceId() != null ? command.deviceId() : existingToken.getDeviceId())
                .build();
        refreshTokenRepository.save(newRefreshToken);

        return AuthResult.bearer(
                newAccessToken,
                newRawRefreshToken,
                tokenService.getAccessTokenExpirationSeconds(),
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getName()
        );
    }
}
