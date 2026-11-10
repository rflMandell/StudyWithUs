package com.studywithus.social.infrastructure;

import com.studywithus.social.domain.Comment;
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
@Table(name = "comments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentJpaEntity {

    @Id
    private UUID id;

    @Column(name = "checkin_id", nullable = false)
    private UUID checkinId;

    @Column(name = "author_id", nullable = false)
    private UUID authorId;

    @Column(nullable = false, length = 1000)
    private String content;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public Comment toDomain() {
        return Comment.builder()
                .id(this.id)
                .checkinId(this.checkinId)
                .authorId(this.authorId)
                .content(this.content)
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt)
                .deletedAt(this.deletedAt)
                .build();
    }

    public static CommentJpaEntity fromDomain(Comment comment) {
        return CommentJpaEntity.builder()
                .id(comment.getId())
                .checkinId(comment.getCheckinId())
                .authorId(comment.getAuthorId())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .deletedAt(comment.getDeletedAt())
                .build();
    }
}
