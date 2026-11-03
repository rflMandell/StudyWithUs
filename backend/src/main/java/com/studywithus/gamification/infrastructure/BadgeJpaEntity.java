package com.studywithus.gamification.infrastructure;

import com.studywithus.gamification.domain.Badge;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import java.util.UUID;

@Entity
@Table(name = "badges")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BadgeJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Badge toDomain() {
        return Badge.builder()
                .id(id)
                .name(name)
                .description(description)
                .imageUrl(imageUrl)
                .createdAt(createdAt)
                .build();
    }

    public static BadgeJpaEntity fromDomain(Badge badge) {
        return BadgeJpaEntity.builder()
                .id(badge.getId())
                .name(badge.getName())
                .description(badge.getDescription())
                .imageUrl(badge.getImageUrl())
                .createdAt(badge.getCreatedAt() != null ? badge.getCreatedAt() : Instant.now())
                .build();
    }
}
