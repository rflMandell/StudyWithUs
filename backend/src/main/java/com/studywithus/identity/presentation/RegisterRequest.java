package com.studywithus.identity.presentation;

import com.studywithus.identity.application.RegisterUserCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record RegisterRequest(
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 30, message = "Username must be between 3 and 30 characters")
        @Pattern(regexp = "^[a-zA-Z0-9_.]+$", message = "Username can only contain letters, numbers, dots and underscores")
        String username,

        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must not exceed 100 characters")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        @Size(max = 255, message = "Email must not exceed 255 characters")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 6, max = 100, message = "Password must be at least 6 characters")
        String password,

        LocalDate birthDate,
        String institution,
        String course,
        String studyGoal,
        String bio,
        String deviceId
) {
    public RegisterUserCommand toCommand() {
        return new RegisterUserCommand(
                username,
                name,
                email,
                password,
                birthDate,
                institution,
                course,
                studyGoal,
                bio,
                deviceId
        );
    }
}
