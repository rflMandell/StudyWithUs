package com.studywithus.identity.application;

import com.studywithus.identity.domain.RefreshToken;
import com.studywithus.identity.domain.RefreshTokenRepository;
import com.studywithus.identity.domain.User;
import com.studywithus.identity.domain.UserCredential;
import com.studywithus.identity.domain.UserRepository;
import com.studywithus.identity.domain.UserStatus;
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
    private final TotpPort totpPort;

    @Override
    @Transactional
    public AuthResult execute(LoginCommand command) {
        String identifier = command.usernameOrEmail().trim().toLowerCase();

        User user = userRepository.findByUsernameOrEmail(identifier)
                .orElseThrow(() -> new DomainException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid username/email or password"));

        if (user.getStatus() == UserStatus.SUSPENDED
                || user.getStatus() == UserStatus.DELETED
                || user.getDeletedAt() != null) {
            throw new DomainException(HttpStatus.FORBIDDEN, "USER_SUSPENDED", "User account is suspended or deleted");
        }

        UserCredential credential = userRepository.findCredentialByUserId(user.getId())
                .orElseThrow(() -> new DomainException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid username/email or password"));

        if (!passwordHasher.matches(command.password(), credential.getPasswordHash())) {
            throw new DomainException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid username/email or password");
        }

        boolean twoFactorVerified = false;
        if (user.getEffectiveGlobalRole().requiresTwoFactorEligibility() && credential.isTotpEnabled()) {
            if (command.totpCode() == null || command.totpCode().isBlank()) {
                throw new DomainException(HttpStatus.UNAUTHORIZED, "TOTP_REQUIRED", "Two-factor authentication code is required for this account");
            }
            if (totpPort == null || !totpPort.verifyCode(credential.getTotpSecret(), command.totpCode())) {
                throw new DomainException(HttpStatus.UNAUTHORIZED, "INVALID_TOTP_CODE", "Invalid two-factor authentication code");
            }
            twoFactorVerified = true;
        }

        Instant now = Instant.now();
        if (user.isDeactivated()) {
            user.setStatus(UserStatus.ACTIVE);
            user.setDeactivatedAt(null);
            user.setUpdatedAt(now);
            userRepository.save(user);
        }
        String accessToken = twoFactorVerified
                ? tokenService.generateAccessToken(user, true)
                : tokenService.generateAccessToken(user);
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

        long expiresIn = user.getEffectiveGlobalRole().isAdminAccess()
                ? tokenService.getAccessTokenExpirationSeconds(user)
                : tokenService.getAccessTokenExpirationSeconds();

        return AuthResult.bearer(
                accessToken,
                rawRefreshToken,
                expiresIn,
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getName(),
                user.getEffectiveGlobalRole()
        );
    }
}
