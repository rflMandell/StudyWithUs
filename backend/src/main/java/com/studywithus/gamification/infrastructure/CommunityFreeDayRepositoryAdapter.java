package com.studywithus.gamification.infrastructure;

import com.studywithus.gamification.domain.CommunityFreeDay;
import com.studywithus.gamification.domain.CommunityFreeDayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CommunityFreeDayRepositoryAdapter implements CommunityFreeDayRepository {

    private final SpringDataCommunityFreeDayRepository repository;

    @Override
    public CommunityFreeDay save(CommunityFreeDay freeDay) {
        CommunityFreeDayJpaEntity entity = CommunityFreeDayJpaEntity.fromDomain(freeDay);
        return repository.save(entity).toDomain();
    }

    @Override
    public List<CommunityFreeDay> findByCommunityId(UUID communityId) {
        return repository.findByCommunityIdOrderByDayOfWeekAsc(communityId).stream()
                .map(CommunityFreeDayJpaEntity::toDomain)
                .toList();
    }

    @Override
    public List<Integer> findDaysOfWeekByCommunityId(UUID communityId) {
        return repository.findDaysOfWeekByCommunityId(communityId);
    }

    @Override
    public boolean existsByCommunityIdAndDayOfWeek(UUID communityId, int dayOfWeek) {
        return repository.existsByCommunityIdAndDayOfWeek(communityId, dayOfWeek);
    }

    @Override
    public void deleteByCommunityIdAndDayOfWeek(UUID communityId, int dayOfWeek) {
        repository.deleteByCommunityIdAndDayOfWeek(communityId, dayOfWeek);
    }
}
