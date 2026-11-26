package com.studywithus.checkin.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "checkin_likes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckinLikeJpaEntity {

    @EmbeddedId
    private CheckinLikeId id;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public static CheckinLikeJpaEntity create(UUID checkinId, UUID userId) {
        return CheckinLikeJpaEntity.builder()
                .id(new CheckinLikeId(checkinId, userId))
                .createdAt(Instant.now())
                .build();
    }
}
