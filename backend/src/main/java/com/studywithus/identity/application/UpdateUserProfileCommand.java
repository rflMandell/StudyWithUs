package com.studywithus.identity.application;

import java.time.LocalDate;
import java.util.UUID;

public record UpdateUserProfileCommand(
        UUID userId,
        String name,
        String bio,
        String institution,
        String course,
        String studyGoal,
        String avatarUrl,
        LocalDate birthDate
) {
}
