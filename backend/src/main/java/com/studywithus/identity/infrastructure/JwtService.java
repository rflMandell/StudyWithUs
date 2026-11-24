package com.studywithus.identity.infrastructure;

import com.studywithus.identity.application.TokenServicePort;
import com.studywithus.identity.domain.GlobalRole;
import com.studywithus.identity.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Service
public class JwtService implements TokenServicePort {

    private final SecretKey signingKey;
    private final long accessTokenExpirationMillis;
    private final long adminAccessTokenExpirationMillis;
    private final long refreshTokenExpirationMillis;
    private final TokenHashService tokenHashService;
    private final SecureRandom secureRandom = new SecureRandom();

    public JwtService(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.access-token-expiration:900000}") long accessTokenExpirationMillis,
            @Value("${security.jwt.admin-access-token-expiration:300000}") long adminAccessTokenExpirationMillis,
            @Value("${security.jwt.refresh-token-expiration:604800000}") long refreshTokenExpirationMillis,
            TokenHashService tokenHashService
    ) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationMillis = accessTokenExpirationMillis;
        this.adminAccessTokenExpirationMillis = adminAccessTokenExpirationMillis;
        this.refreshTokenExpirationMillis = refreshTokenExpirationMillis;
        this.tokenHashService = tokenHashService;
    }

    @Override
    public String generateAccessToken(User user) {
        return generateAccessToken(user, false);
    }

    @Override
    public String generateAccessToken(User user, boolean twoFactorVerified) {
        Instant now = Instant.now();
        GlobalRole globalRole = user.getEffectiveGlobalRole();
        long ttlMillis = globalRole.isAdminAccess() ? adminAccessTokenExpirationMillis : accessTokenExpirationMillis;
        Instant expiry = now.plusMillis(ttlMillis);

        return Jwts.builder()
                .subject(user.getId().toString())
                .claims(Map.of(
                        "username", user.getUsername(),
                        "email", user.getEmail(),
                        "name", user.getName(),
                        "globalRole", globalRole.name(),
                        "twoFactorVerified", twoFactorVerified
                ))
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey)
                .compact();
    }

    @Override
    public String generateRefreshToken() {
        byte[] randomBytes = new byte[48];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    @Override
    public long getAccessTokenExpirationSeconds() {
        return accessTokenExpirationMillis / 1000;
    }

    @Override
    public long getAccessTokenExpirationSeconds(User user) {
        long ttlMillis = user.getEffectiveGlobalRole().isAdminAccess()
                ? adminAccessTokenExpirationMillis
                : accessTokenExpirationMillis;
        return ttlMillis / 1000;
    }

    @Override
    public long getRefreshTokenExpirationSeconds() {
        return refreshTokenExpirationMillis / 1000;
    }

    @Override
    public String hashToken(String token) {
        return tokenHashService.hash(token);
    }

    @Override
    public boolean validateRefreshToken(String token) {
        return token != null && !token.isBlank();
    }

    public UUID extractUserId(String token) {
        String sub = extractClaim(token, Claims::getSubject);
        return UUID.fromString(sub);
    }

    public String extractUsername(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("username", String.class);
    }

    public GlobalRole extractGlobalRole(String token) {
        Claims claims = extractAllClaims(token);
        String role = claims.get("globalRole", String.class);
        if (role == null || role.isBlank()) {
            return GlobalRole.USER;
        }
        try {
            return GlobalRole.valueOf(role);
        } catch (IllegalArgumentException e) {
            return GlobalRole.USER;
        }
    }

    public boolean isTwoFactorVerified(String token) {
        Claims claims = extractAllClaims(token);
        Boolean verified = claims.get("twoFactorVerified", Boolean.class);
        return Boolean.TRUE.equals(verified);
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractAllClaims(token);
            Date expiration = claims.getExpiration();
            return expiration != null && expiration.after(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
