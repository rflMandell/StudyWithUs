package com.studywithus.identity.application;

import com.studywithus.identity.domain.RefreshToken;
import com.studywithus.identity.domain.RefreshTokenRepository;
import com.studywithus.identity.domain.User;
import com.studywithus.identity.domain.UserCredential;
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
class LoginUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordHasher passwordHasher;

    @Mock
    private TokenServicePort tokenService;

    @InjectMocks
    private LoginUseCase loginUseCase;

    private User testUser;
    private UserCredential testCredential;

    @BeforeEach
    void setUp() {
        UUID userId = UUID.randomUUID();
        testUser = User.builder()
                .id(userId)
                .username("john_doe")
                .name("John Doe")
                .email("john@example.com")
                .status(UserStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        testCredential = UserCredential.builder()
                .userId(userId)
                .passwordHash("hashed-secret")
                .build();
    }

    @Test
    @DisplayName("Should successfully authenticate user with correct credentials")
    void loginSuccessfully() {
        LoginCommand command = new LoginCommand("john_doe", "password123", "device-x");

        when(userRepository.findByUsernameOrEmail("john_doe")).thenReturn(Optional.of(testUser));
        when(userRepository.findCredentialByUserId(testUser.getId())).thenReturn(Optional.of(testCredential));
        when(passwordHasher.matches("password123", "hashed-secret")).thenReturn(true);
        when(tokenService.generateAccessToken(testUser)).thenReturn("jwt-token-1");
        when(tokenService.generateRefreshToken()).thenReturn("refresh-token-1");
        when(tokenService.hashToken("refresh-token-1")).thenReturn("hashed-refresh-token");
        when(tokenService.getAccessTokenExpirationSeconds()).thenReturn(900L);
        when(tokenService.getRefreshTokenExpirationSeconds()).thenReturn(604800L);

        AuthResult result = loginUseCase.execute(command);

        assertThat(result).isNotNull();
        assertThat(result.accessToken()).isEqualTo("jwt-token-1");
        assertThat(result.refreshToken()).isEqualTo("refresh-token-1");
        assertThat(result.username()).isEqualTo("john_doe");

        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Should throw unauthorized when password does not match")
    void throwWhenPasswordIncorrect() {
        LoginCommand command = new LoginCommand("john_doe", "wrongpassword", null);

        when(userRepository.findByUsernameOrEmail("john_doe")).thenReturn(Optional.of(testUser));
        when(userRepository.findCredentialByUserId(testUser.getId())).thenReturn(Optional.of(testCredential));
        when(passwordHasher.matches("wrongpassword", "hashed-secret")).thenReturn(false);

        assertThatThrownBy(() -> loginUseCase.execute(command))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> {
                    DomainException de = (DomainException) ex;
                    assertThat(de.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(de.getCode()).isEqualTo("INVALID_CREDENTIALS");
                });
    }

    @Test
    @DisplayName("Should throw unauthorized when user not found")
    void throwWhenUserNotFound() {
        LoginCommand command = new LoginCommand("unknown_user", "password123", null);

        when(userRepository.findByUsernameOrEmail("unknown_user")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loginUseCase.execute(command))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> {
                    DomainException de = (DomainException) ex;
                    assertThat(de.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(de.getCode()).isEqualTo("INVALID_CREDENTIALS");
                });
    }

    @Test
    @DisplayName("Should throw forbidden when user account is suspended")
    void throwWhenUserSuspended() {
        testUser.setStatus(UserStatus.SUSPENDED);
        LoginCommand command = new LoginCommand("john_doe", "password123", null);

        when(userRepository.findByUsernameOrEmail("john_doe")).thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> loginUseCase.execute(command))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> {
                    DomainException de = (DomainException) ex;
                    assertThat(de.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(de.getCode()).isEqualTo("USER_SUSPENDED");
                });
    }
}
