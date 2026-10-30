package com.studywithus.identity.application;

import com.studywithus.identity.domain.User;
import com.studywithus.identity.domain.UserStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record UserProfileResult(
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
    public static UserProfileResult from(User user) {
        return new UserProfileResult(
                user.getId(),
                user.getUsername(),
                user.getName(),
                user.getEmail(),
                user.getBirthDate(),
                user.getAvatarUrl(),
                user.getInstitution(),
                user.getCourse(),
                user.getStudyGoal(),
                user.getBio(),
                user.getStatus(),
                user.getLifetimePoints(),
                user.getCreatedAt()
        );
    }
}
