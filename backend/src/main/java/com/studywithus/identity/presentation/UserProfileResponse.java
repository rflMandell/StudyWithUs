package com.studywithus.identity.presentation;

import com.studywithus.identity.application.UserProfileResult;
import com.studywithus.identity.domain.UserStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String username,
        String name,
        String email,
        LocalDate birthDate,
        String avatarUrl,
        String institution,
        String course,
        String studyGoal,
        String bio,
        UserStatus status,
        long lifetimePoints,
        Instant createdAt
) {
    public static UserProfileResponse from(UserProfileResult result) {
        return new UserProfileResponse(
                result.id(),
                result.username(),
                result.name(),
                result.email(),
                result.birthDate(),
                result.avatarUrl(),
                result.institution(),
                result.course(),
                result.studyGoal(),
                result.bio(),
                result.status(),
                result.lifetimePoints(),
                result.createdAt()
        );
    }
}
