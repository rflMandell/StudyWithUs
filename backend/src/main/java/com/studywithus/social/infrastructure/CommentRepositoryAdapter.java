package com.studywithus.social.infrastructure;

import com.studywithus.shared.domain.PagedResult;
import com.studywithus.social.domain.Comment;
import com.studywithus.social.domain.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CommentRepositoryAdapter implements CommentRepository {

    private final SpringDataCommentRepository repository;

    @Override
    public Comment save(Comment comment) {
        CommentJpaEntity entity = CommentJpaEntity.fromDomain(comment);
        return repository.save(entity).toDomain();
    }

    @Override
    public Optional<Comment> findById(UUID id) {
        return repository.findActiveById(id).map(CommentJpaEntity::toDomain);
    }

    @Override
    public PagedResult<Comment> findByCheckinId(UUID checkinId, int page, int size) {
        Page<CommentJpaEntity> resultPage = repository.findActiveByCheckinId(
                checkinId,
                PageRequest.of(page, size)
        );

        List<Comment> items = resultPage.getContent().stream()
                .map(CommentJpaEntity::toDomain)
                .toList();

        return new PagedResult<>(
                items,
                resultPage.getNumber(),
                resultPage.getSize(),
                resultPage.getTotalElements(),
                resultPage.getTotalPages(),
                resultPage.hasNext()
        );
    }

    @Override
    public long countByCheckinId(UUID checkinId) {
        return repository.countActiveByCheckinId(checkinId);
    }

    @Override
    public Map<UUID, Long> countActiveByCheckinIds(Collection<UUID> checkinIds) {
        if (checkinIds == null || checkinIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Object[]> rows = repository.countActiveGroupedByCheckinIds(checkinIds);
        Map<UUID, Long> counts = new HashMap<>();
        for (Object[] row : rows) {
            UUID checkinId = (UUID) row[0];
            Long count = ((Number) row[1]).longValue();
            counts.put(checkinId, count);
        }
        return counts;
    }
}
