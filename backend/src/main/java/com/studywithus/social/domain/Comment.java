package com.studywithus.social.domain;

import com.studywithus.shared.domain.DomainException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Comment {
    private UUID id;
    private UUID checkinId;
    private UUID authorId;
    private String content;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;

    public static Comment create(UUID checkinId, UUID authorId, String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "COMMENT_EMPTY", "O comentário não pode ser vazio");
        }
        String trimmed = content.trim();
        if (trimmed.length() > 1000) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "COMMENT_TOO_LONG", "O comentário deve ter no máximo 1000 caracteres");
        }

        Instant now = Instant.now();
        return Comment.builder()
                .id(UUID.randomUUID())
                .checkinId(checkinId)
                .authorId(authorId)
                .content(trimmed)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public boolean isActive() {
        return deletedAt == null;
    }

    public boolean isAuthor(UUID targetUserId) {
        return this.authorId != null && this.authorId.equals(targetUserId);
    }

    public void delete(UUID requestingUserId) {
        if (!isAuthor(requestingUserId)) {
            throw new DomainException(HttpStatus.FORBIDDEN, "COMMENT_FORBIDDEN", "Apenas o autor pode excluir o comentário");
        }
        Instant now = Instant.now();
        this.deletedAt = now;
        this.updatedAt = now;
    }
}
