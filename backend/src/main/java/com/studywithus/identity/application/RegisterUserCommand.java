package com.studywithus.identity.application;

import java.time.LocalDate;

public record RegisterUserCommand(
        String username,
        String name,
        String email,
        String password,
        LocalDate birthDate,
        String institution,
        String course,
        String studyGoal,
        String bio,
        String deviceId
) {
}
