package com.studywithus.social.infrastructure;

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
@Table(name = "comment_likes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentLikeJpaEntity {

    @EmbeddedId
    private CommentLikeId id;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public static CommentLikeJpaEntity create(UUID commentId, UUID userId) {
        return CommentLikeJpaEntity.builder()
                .id(new CommentLikeId(commentId, userId))
                .createdAt(Instant.now())
                .build();
    }
}
