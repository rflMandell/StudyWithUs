package com.studywithus.identity.application;

import com.studywithus.identity.domain.User;

import java.util.UUID;

public interface TokenServicePort {
    String generateAccessToken(User user);
    String generateRefreshToken();
    long getAccessTokenExpirationSeconds();
    long getRefreshTokenExpirationSeconds();
    String hashToken(String token);
    boolean validateRefreshToken(String token);
}
