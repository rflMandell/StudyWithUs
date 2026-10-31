package com.studywithus.community.infrastructure;

import com.studywithus.community.domain.Community;
import com.studywithus.community.domain.CommunityVisibility;
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
import java.util.UUID;

@Entity
@Table(name = "communities")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CommunityVisibility visibility;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "ranking_type", nullable = false, length = 50)
    private String rankingType;

    @Column(nullable = false, length = 50)
    private String timezone;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public Community toDomain() {
        return Community.builder()
                .id(id)
                .name(name)
                .description(description)
                .imageUrl(imageUrl)
                .visibility(visibility)
                .ownerId(ownerId)
                .rankingType(rankingType)
                .timezone(timezone)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .deletedAt(deletedAt)
                .build();
    }

    public static CommunityJpaEntity fromDomain(Community community) {
        return CommunityJpaEntity.builder()
                .id(community.getId())
                .name(community.getName())
                .description(community.getDescription())
                .imageUrl(community.getImageUrl())
                .visibility(community.getVisibility())
                .ownerId(community.getOwnerId())
                .rankingType(community.getRankingType() != null ? community.getRankingType() : "WEEKLY")
                .timezone(community.getTimezone() != null ? community.getTimezone() : "UTC")
                .createdAt(community.getCreatedAt() != null ? community.getCreatedAt() : Instant.now())
                .updatedAt(community.getUpdatedAt() != null ? community.getUpdatedAt() : Instant.now())
                .deletedAt(community.getDeletedAt())
                .build();
    }
}
