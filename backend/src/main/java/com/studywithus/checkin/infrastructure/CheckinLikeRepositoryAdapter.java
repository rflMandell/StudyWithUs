package com.studywithus.checkin.infrastructure;

import com.studywithus.checkin.domain.CheckinLikeRepository;
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
public class CheckinLikeRepositoryAdapter implements CheckinLikeRepository {

    private final SpringDataCheckinLikeRepository repository;

    @Override
    public boolean exists(UUID checkinId, UUID userId) {
        return repository.existsById(new CheckinLikeId(checkinId, userId));
    }

    @Override
    public void save(UUID checkinId, UUID userId) {
        repository.save(CheckinLikeJpaEntity.create(checkinId, userId));
    }

    @Override
    public void delete(UUID checkinId, UUID userId) {
        repository.deleteById(new CheckinLikeId(checkinId, userId));
    }

    @Override
    public long countByCheckinId(UUID checkinId) {
        return repository.countByCheckinId(checkinId);
    }

    @Override
    public Map<UUID, Long> countByCheckinIds(Collection<UUID> checkinIds) {
        if (checkinIds == null || checkinIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Object[]> rows = repository.countGroupedByCheckinIds(checkinIds);
        Map<UUID, Long> counts = new HashMap<>();
        for (Object[] row : rows) {
            counts.put((UUID) row[0], ((Number) row[1]).longValue());
        }
        return counts;
    }

    @Override
    public Set<UUID> findLikedCheckinIdsByUser(Collection<UUID> checkinIds, UUID userId) {
        if (checkinIds == null || checkinIds.isEmpty() || userId == null) {
            return Collections.emptySet();
        }
        return repository.findLikedCheckinIdsByUser(checkinIds, userId);
    }
}
