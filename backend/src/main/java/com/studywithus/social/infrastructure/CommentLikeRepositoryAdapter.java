package com.studywithus.social.infrastructure;

import com.studywithus.social.domain.CommentLikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CommentLikeRepositoryAdapter implements CommentLikeRepository {

    private final SpringDataCommentLikeRepository repository;

    @Override
    public boolean exists(UUID commentId, UUID userId) {
        return repository.existsById(new CommentLikeId(commentId, userId));
    }

    @Override
    public void save(UUID commentId, UUID userId) {
        repository.save(CommentLikeJpaEntity.create(commentId, userId));
    }

    @Override
    public void delete(UUID commentId, UUID userId) {
        repository.deleteById(new CommentLikeId(commentId, userId));
    }

    @Override
    public long countByCommentId(UUID commentId) {
        return repository.countByCommentId(commentId);
    }

    @Override
    public Map<UUID, Long> countByCommentIds(Collection<UUID> commentIds) {
        if (commentIds == null || commentIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Object[]> rows = repository.countGroupedByCommentIds(commentIds);
        Map<UUID, Long> counts = new HashMap<>();
        for (Object[] row : rows) {
            counts.put((UUID) row[0], ((Number) row[1]).longValue());
        }
        return counts;
    }

    @Override
    public Set<UUID> findLikedCommentIdsByUser(Collection<UUID> commentIds, UUID userId) {
        if (commentIds == null || commentIds.isEmpty() || userId == null) {
            return Collections.emptySet();
        }
        return repository.findLikedCommentIdsByUser(commentIds, userId);
    }
}
