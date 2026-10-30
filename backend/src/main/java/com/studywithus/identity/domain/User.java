package com.studywithus.identity.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private UUID id;
    private String username;
    private String name;
    private String email;
    private LocalDate birthDate;
    private String avatarUrl;
    private String institution;
    private String course;
    private String studyGoal;
    private String bio;
    private UserStatus status;
    private long lifetimePoints;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;

    public boolean isActive() {
        return status == UserStatus.ACTIVE && deletedAt == null;
    }
}
