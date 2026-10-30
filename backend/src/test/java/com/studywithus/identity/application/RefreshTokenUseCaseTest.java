package com.studywithus.identity.application;

import com.studywithus.identity.domain.RefreshToken;
import com.studywithus.identity.domain.RefreshTokenRepository;
import com.studywithus.identity.domain.User;
import com.studywithus.identity.domain.UserRepository;
import com.studywithus.identity.domain.UserStatus;
import com.studywithus.shared.domain.DomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private TokenServicePort tokenService;

    @InjectMocks
    private RefreshTokenUseCase refreshTokenUseCase;

    private User testUser;
    private RefreshToken validRefreshToken;

    @BeforeEach
    void setUp() {
        UUID userId = UUID.randomUUID();
        testUser = User.builder()
                .id(userId)
                .username("sarah")
                .name("Sarah Connor")
                .email("sarah@example.com")
                .status(UserStatus.ACTIVE)
                .build();

        validRefreshToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .tokenHash("hash-existing")
                .expiresAt(Instant.now().plusSeconds(3600))
                .createdAt(Instant.now().minusSeconds(3600))
                .deviceId("device-1")
                .build();
    }

    @Test
    @DisplayName("Should rotate refresh token and return new token pair")
    void refreshTokenSuccessfully() {
        RefreshTokenCommand command = new RefreshTokenCommand("raw-refresh-token", "device-1");

        when(tokenService.hashToken("raw-refresh-token")).thenReturn("hash-existing");
        when(refreshTokenRepository.findByTokenHash("hash-existing")).thenReturn(Optional.of(validRefreshToken));
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(tokenService.generateAccessToken(testUser)).thenReturn("new-access-token");
        when(tokenService.generateRefreshToken()).thenReturn("new-raw-refresh-token");
        when(tokenService.hashToken("new-raw-refresh-token")).thenReturn("new-hash");
        when(tokenService.getAccessTokenExpirationSeconds()).thenReturn(900L);
        when(tokenService.getRefreshTokenExpirationSeconds()).thenReturn(604800L);

        AuthResult result = refreshTokenUseCase.execute(command);

        assertThat(result).isNotNull();
        assertThat(result.accessToken()).isEqualTo("new-access-token");
        assertThat(result.refreshToken()).isEqualTo("new-raw-refresh-token");
        assertThat(validRefreshToken.isRevoked()).isTrue();

        verify(refreshTokenRepository).save(validRefreshToken);
        verify(refreshTokenRepository, org.mockito.Mockito.times(2)).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Should throw unauthorized when token is expired or revoked")
    void throwWhenTokenExpiredOrRevoked() {
        validRefreshToken.revoke();
        RefreshTokenCommand command = new RefreshTokenCommand("raw-refresh-token", null);

        when(tokenService.hashToken("raw-refresh-token")).thenReturn("hash-existing");
        when(refreshTokenRepository.findByTokenHash("hash-existing")).thenReturn(Optional.of(validRefreshToken));

        assertThatThrownBy(() -> refreshTokenUseCase.execute(command))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> {
                    DomainException de = (DomainException) ex;
                    assertThat(de.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(de.getCode()).isEqualTo("REFRESH_TOKEN_EXPIRED_OR_REVOKED");
                });
    }

    @Test
    @DisplayName("Should throw unauthorized when token hash not found")
    void throwWhenTokenNotFound() {
        RefreshTokenCommand command = new RefreshTokenCommand("unknown-token", null);

        when(tokenService.hashToken("unknown-token")).thenReturn("unknown-hash");
        when(refreshTokenRepository.findByTokenHash("unknown-hash")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenUseCase.execute(command))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> {
                    DomainException de = (DomainException) ex;
                    assertThat(de.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(de.getCode()).isEqualTo("INVALID_REFRESH_TOKEN");
                });
    }
}
