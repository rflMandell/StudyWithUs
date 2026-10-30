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
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RegisterUserUseCase implements UseCase<RegisterUserCommand, AuthResult> {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordHasher passwordHasher;
    private final TokenServicePort tokenService;

    @Override
    @Transactional
    public AuthResult execute(RegisterUserCommand command) {
        String cleanUsername = command.username().trim().toLowerCase();
        String cleanEmail = command.email().trim().toLowerCase();

        if (userRepository.existsByUsername(cleanUsername)) {
            throw new DomainException(HttpStatus.CONFLICT, "USERNAME_ALREADY_EXISTS", "Username is already taken");
        }

        if (userRepository.existsByEmail(cleanEmail)) {
            throw new DomainException(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS", "Email is already registered");
        }

        Instant now = Instant.now();
        User user = User.builder()
                .username(cleanUsername)
                .name(command.name().trim())
                .email(cleanEmail)
                .birthDate(command.birthDate())
                .institution(command.institution())
                .course(command.course())
                .studyGoal(command.studyGoal())
                .bio(command.bio())
                .status(UserStatus.ACTIVE)
                .lifetimePoints(0)
                .createdAt(now)
                .updatedAt(now)
                .build();

        User savedUser = userRepository.save(user);

        UserCredential credential = UserCredential.builder()
                .userId(savedUser.getId())
                .passwordHash(passwordHasher.hash(command.password()))
                .createdAt(now)
                .updatedAt(now)
                .build();
        userRepository.saveCredential(credential);

        String accessToken = tokenService.generateAccessToken(savedUser);
        String rawRefreshToken = tokenService.generateRefreshToken();
        String tokenHash = tokenService.hashToken(rawRefreshToken);
        Instant refreshTokenExpiresAt = now.plusSeconds(tokenService.getRefreshTokenExpirationSeconds());

        RefreshToken refreshToken = RefreshToken.builder()
                .userId(savedUser.getId())
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
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.getName()
        );
    }
}
