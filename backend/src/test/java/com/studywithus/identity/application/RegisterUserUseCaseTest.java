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

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterUserUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordHasher passwordHasher;

    @Mock
    private TokenServicePort tokenService;

    @InjectMocks
    private RegisterUserUseCase registerUserUseCase;

    private RegisterUserCommand validCommand;

    @BeforeEach
    void setUp() {
        validCommand = new RegisterUserCommand(
                "student1",
                "Alice Student",
                "alice@university.edu",
                "password123",
                LocalDate.of(2000, 1, 1),
                "MIT",
                "Computer Science",
                "Pass finals",
                "Study daily",
                "device-01"
        );
    }

    @Test
    @DisplayName("Should successfully register a new user")
    void registerUserSuccessfully() {
        when(userRepository.existsByUsername("student1")).thenReturn(false);
        when(userRepository.existsByEmail("alice@university.edu")).thenReturn(false);
        when(passwordHasher.hash("password123")).thenReturn("hashed-pwd");

        UUID generatedId = UUID.randomUUID();
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(generatedId);
            return u;
        });

        when(tokenService.generateAccessToken(any(User.class))).thenReturn("access-token-123");
        when(tokenService.generateRefreshToken()).thenReturn("refresh-token-456");
        when(tokenService.hashToken("refresh-token-456")).thenReturn("hashed-refresh-token");
        when(tokenService.getAccessTokenExpirationSeconds()).thenReturn(900L);
        when(tokenService.getRefreshTokenExpirationSeconds()).thenReturn(604800L);

        AuthResult result = registerUserUseCase.execute(validCommand);

        assertThat(result).isNotNull();
        assertThat(result.accessToken()).isEqualTo("access-token-123");
        assertThat(result.refreshToken()).isEqualTo("refresh-token-456");
        assertThat(result.username()).isEqualTo("student1");
        assertThat(result.email()).isEqualTo("alice@university.edu");

        verify(userRepository).save(any(User.class));
        verify(userRepository).saveCredential(any(UserCredential.class));
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Should throw conflict exception when username already exists")
    void throwWhenUsernameExists() {
        when(userRepository.existsByUsername("student1")).thenReturn(true);

        assertThatThrownBy(() -> registerUserUseCase.execute(validCommand))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> {
                    DomainException de = (DomainException) ex;
                    assertThat(de.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(de.getCode()).isEqualTo("USERNAME_ALREADY_EXISTS");
                });

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw conflict exception when email already exists")
    void throwWhenEmailExists() {
        when(userRepository.existsByUsername("student1")).thenReturn(false);
        when(userRepository.existsByEmail("alice@university.edu")).thenReturn(true);

        assertThatThrownBy(() -> registerUserUseCase.execute(validCommand))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> {
                    DomainException de = (DomainException) ex;
                    assertThat(de.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(de.getCode()).isEqualTo("EMAIL_ALREADY_EXISTS");
                });

        verify(userRepository, never()).save(any(User.class));
    }
}
