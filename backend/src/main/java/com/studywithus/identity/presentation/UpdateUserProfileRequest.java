package com.studywithus.identity.presentation;

import com.studywithus.identity.application.UpdateUserProfileCommand;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record UpdateUserProfileRequest(
        @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
        String name,

        @Size(max = 500, message = "Bio must not exceed 500 characters")
        String bio,

        @Size(max = 150, message = "Institution must not exceed 150 characters")
        String institution,

        @Size(max = 150, message = "Course must not exceed 150 characters")
        String course,

        @Size(max = 255, message = "Study goal must not exceed 255 characters")
        String studyGoal,

        String avatarUrl,

        LocalDate birthDate
) {
    public UpdateUserProfileCommand toCommand(UUID userId) {
        return new UpdateUserProfileCommand(
                userId,
                name,
                bio,
                institution,
                course,
                studyGoal,
                avatarUrl,
                birthDate
        );
    }
}
