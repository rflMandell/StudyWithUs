package com.studywithus.identity.infrastructure;

import com.studywithus.identity.domain.UserCredential;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_credentials")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCredentialJpaEntity {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UserCredential toDomain() {
        return UserCredential.builder()
                .userId(userId)
                .passwordHash(passwordHash)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }

    public static UserCredentialJpaEntity fromDomain(UserCredential credential) {
        return UserCredentialJpaEntity.builder()
                .userId(credential.getUserId())
                .passwordHash(credential.getPasswordHash())
                .createdAt(credential.getCreatedAt() != null ? credential.getCreatedAt() : Instant.now())
                .updatedAt(credential.getUpdatedAt() != null ? credential.getUpdatedAt() : Instant.now())
                .build();
    }
}
