package com.studywithus.identity.infrastructure;

import com.studywithus.identity.domain.User;
import com.studywithus.identity.domain.UserStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 30)
    private String username;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "avatar_url", columnDefinition = "TEXT")
    private String avatarUrl;

    @Column(length = 150)
    private String institution;

    @Column(length = 150)
    private String course;

    @Column(name = "study_goal", length = 255)
    private String studyGoal;

    @Column(length = 500)
    private String bio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    @Column(name = "lifetime_points", nullable = false)
    private long lifetimePoints;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public User toDomain() {
        return User.builder()
                .id(id)
                .username(username)
                .name(name)
                .email(email)
                .birthDate(birthDate)
                .avatarUrl(avatarUrl)
                .institution(institution)
                .course(course)
                .studyGoal(studyGoal)
                .bio(bio)
                .status(status)
                .lifetimePoints(lifetimePoints)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .deletedAt(deletedAt)
                .build();
    }

    public static UserJpaEntity fromDomain(User user) {
        return UserJpaEntity.builder()
                .id(user.getId())
                .username(user.getUsername())
                .name(user.getName())
                .email(user.getEmail())
                .birthDate(user.getBirthDate())
                .avatarUrl(user.getAvatarUrl())
                .institution(user.getInstitution())
                .course(user.getCourse())
                .studyGoal(user.getStudyGoal())
                .bio(user.getBio())
                .status(user.getStatus())
                .lifetimePoints(user.getLifetimePoints())
                .createdAt(user.getCreatedAt() != null ? user.getCreatedAt() : Instant.now())
                .updatedAt(user.getUpdatedAt() != null ? user.getUpdatedAt() : Instant.now())
                .deletedAt(user.getDeletedAt())
                .build();
    }
}
