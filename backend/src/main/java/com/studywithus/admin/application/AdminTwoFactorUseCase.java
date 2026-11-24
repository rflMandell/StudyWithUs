package com.studywithus.admin.application;

import com.studywithus.identity.application.TokenServicePort;
import com.studywithus.identity.application.TotpPort;
import com.studywithus.identity.domain.User;
import com.studywithus.identity.domain.UserCredential;
import com.studywithus.identity.domain.UserRepository;
import com.studywithus.shared.domain.DomainException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminTwoFactorUseCase {

    private final UserRepository userRepository;
    private final TotpPort totpPort;
    private final TokenServicePort tokenService;

    @Transactional(readOnly = true)
    public AdminSessionResult getSessionInfo(UUID userId) {
        User user = requireActiveAdmin(userId);
        boolean totpEnabled = userRepository.findCredentialByUserId(userId)
                .map(UserCredential::isTotpEnabled)
                .orElse(false);

        return new AdminSessionResult(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getName(),
                user.getEffectiveGlobalRole(),
                totpEnabled,
                user.getEffectiveGlobalRole().requiresTwoFactorEligibility(),
                tokenService.getAccessTokenExpirationSeconds(user)
        );
    }

    @Transactional
    public TwoFactorSetupResult setupTwoFactor(UUID userId) {
        User user = requireEligibleTwoFactorAdmin(userId);
        UserCredential credential = userRepository.findCredentialByUserId(userId)
                .orElseThrow(() -> new DomainException(HttpStatus.BAD_REQUEST, "CREDENTIALS_NOT_FOUND", "Password credentials are required for 2FA"));

        String secret = totpPort.generateSecret();
        credential.setTotpSecret(secret);
        credential.setTotpEnabled(false);
        credential.setTotpVerifiedAt(null);
        credential.setUpdatedAt(Instant.now());
        userRepository.saveCredential(credential);

        String otpAuthUri = totpPort.buildOtpAuthUri(user.getEmail(), secret);
        return new TwoFactorSetupResult(secret, otpAuthUri);
    }

    @Transactional
    public void verifyAndEnableTwoFactor(UUID userId, String code) {
        requireEligibleTwoFactorAdmin(userId);
        UserCredential credential = userRepository.findCredentialByUserId(userId)
                .orElseThrow(() -> new DomainException(HttpStatus.BAD_REQUEST, "CREDENTIALS_NOT_FOUND", "Password credentials are required for 2FA"));

        if (credential.getTotpSecret() == null || credential.getTotpSecret().isBlank()) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "TOTP_NOT_INITIALIZED", "2FA setup must be initiated before verification");
        }

        if (!totpPort.verifyCode(credential.getTotpSecret(), code)) {
            throw new DomainException(HttpStatus.UNAUTHORIZED, "INVALID_TOTP_CODE", "Invalid two-factor authentication code");
        }

        Instant now = Instant.now();
        credential.setTotpEnabled(true);
        credential.setTotpVerifiedAt(now);
        credential.setUpdatedAt(now);
        userRepository.saveCredential(credential);
    }

    @Transactional
    public void disableTwoFactor(UUID userId, String code) {
        requireEligibleTwoFactorAdmin(userId);
        UserCredential credential = userRepository.findCredentialByUserId(userId)
                .orElseThrow(() -> new DomainException(HttpStatus.BAD_REQUEST, "CREDENTIALS_NOT_FOUND", "Password credentials are required for 2FA"));

        if (!credential.isTotpEnabled()) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "TOTP_NOT_ENABLED", "Two-factor authentication is not enabled");
        }

        if (!totpPort.verifyCode(credential.getTotpSecret(), code)) {
            throw new DomainException(HttpStatus.UNAUTHORIZED, "INVALID_TOTP_CODE", "Invalid two-factor authentication code");
        }

        credential.setTotpSecret(null);
        credential.setTotpEnabled(false);
        credential.setTotpVerifiedAt(null);
        credential.setUpdatedAt(Instant.now());
        userRepository.saveCredential(credential);
    }

    private User requireActiveAdmin(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new DomainException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND", "User not found"));
        if (!user.isActive()) {
            throw new DomainException(HttpStatus.FORBIDDEN, "USER_SUSPENDED", "User account is suspended or deleted");
        }
        if (!user.getEffectiveGlobalRole().isAdminAccess()) {
            throw new DomainException(HttpStatus.FORBIDDEN, "ADMIN_ACCESS_REQUIRED", "Administrative role is required");
        }
        return user;
    }

    private User requireEligibleTwoFactorAdmin(UUID userId) {
        User user = requireActiveAdmin(userId);
        if (!user.getEffectiveGlobalRole().requiresTwoFactorEligibility()) {
            throw new DomainException(HttpStatus.FORBIDDEN, "TWO_FACTOR_ROLE_REQUIRED", "2FA management is restricted to ADMIN and SUPER_ADMIN accounts");
        }
        return user;
    }
}
