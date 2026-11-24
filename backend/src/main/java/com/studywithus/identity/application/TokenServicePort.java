package com.studywithus.identity.application;

import com.studywithus.identity.domain.User;

import java.util.UUID;

public interface TokenServicePort {
    String generateAccessToken(User user);
    default String generateAccessToken(User user, boolean twoFactorVerified) {
        return generateAccessToken(user);
    }
    String generateRefreshToken();
    long getAccessTokenExpirationSeconds();
    default long getAccessTokenExpirationSeconds(User user) {
        return getAccessTokenExpirationSeconds();
    }
    long getRefreshTokenExpirationSeconds();
    String hashToken(String token);
    boolean validateRefreshToken(String token);
}
