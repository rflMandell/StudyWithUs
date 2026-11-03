package com.studywithus.social.infrastructure;

import com.studywithus.social.domain.Follow;
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

@Entity
@Table(name = "follows")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FollowJpaEntity {

    @EmbeddedId
    private FollowId id;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public Follow toDomain() {
        return Follow.builder()
                .followerId(id.getFollowerId())
                .followingId(id.getFollowingId())
                .createdAt(createdAt)
                .build();
    }

    public static FollowJpaEntity fromDomain(Follow follow) {
        return FollowJpaEntity.builder()
                .id(new FollowId(follow.getFollowerId(), follow.getFollowingId()))
                .createdAt(follow.getCreatedAt() != null ? follow.getCreatedAt() : Instant.now())
                .build();
    }
}
