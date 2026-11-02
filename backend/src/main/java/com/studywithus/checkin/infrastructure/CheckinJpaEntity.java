package com.studywithus.checkin.infrastructure;

import com.studywithus.checkin.domain.Checkin;
import com.studywithus.checkin.domain.CheckinMedia;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "checkins")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckinJpaEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "community_id", nullable = false)
    private UUID communityId;

    @Column(length = 1000)
    private String caption;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Builder.Default
    @OneToMany(mappedBy = "checkin", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("position ASC")
    private List<CheckinMediaJpaEntity> media = new ArrayList<>();

    public static CheckinJpaEntity fromDomain(Checkin domain) {
        CheckinJpaEntity entity = CheckinJpaEntity.builder()
                .id(domain.getId())
                .userId(domain.getUserId())
                .communityId(domain.getCommunityId())
                .caption(domain.getCaption())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .deletedAt(domain.getDeletedAt())
                .media(new ArrayList<>())
                .build();

        if (domain.getMedia() != null) {
            for (CheckinMedia m : domain.getMedia()) {
                CheckinMediaJpaEntity mediaEntity = CheckinMediaJpaEntity.builder()
                        .id(m.getId())
                        .checkin(entity)
                        .storageKey(m.getStorageKey())
                        .position(m.getPosition())
                        .createdAt(m.getCreatedAt())
                        .build();
                entity.getMedia().add(mediaEntity);
            }
        }

        return entity;
    }

    public Checkin toDomain() {
        List<CheckinMedia> mediaDomain = new ArrayList<>();
        if (this.media != null) {
            for (CheckinMediaJpaEntity m : this.media) {
                mediaDomain.add(CheckinMedia.builder()
                        .id(m.getId())
                        .checkinId(this.id)
                        .storageKey(m.getStorageKey())
                        .position(m.getPosition())
                        .createdAt(m.getCreatedAt())
                        .build());
            }
        }

        return Checkin.builder()
                .id(this.id)
                .userId(this.userId)
                .communityId(this.communityId)
                .caption(this.caption)
                .media(mediaDomain)
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt)
                .deletedAt(this.deletedAt)
                .build();
    }
}
