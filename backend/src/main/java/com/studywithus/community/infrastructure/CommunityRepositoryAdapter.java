package com.studywithus.community.infrastructure;

import com.studywithus.community.domain.Community;
import com.studywithus.community.domain.CommunityRepository;
import com.studywithus.community.domain.CommunityVisibility;
import com.studywithus.shared.domain.PagedResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CommunityRepositoryAdapter implements CommunityRepository {

    private final SpringDataCommunityRepository springDataCommunityRepository;

    @Override
    public Community save(Community community) {
        CommunityJpaEntity entity = CommunityJpaEntity.fromDomain(community);
        return springDataCommunityRepository.save(entity).toDomain();
    }

    @Override
    public Optional<Community> findById(UUID id) {
        return springDataCommunityRepository.findById(id).map(CommunityJpaEntity::toDomain);
    }

    @Override
    public boolean existsByNameIgnoreCase(String name) {
        return springDataCommunityRepository.existsByNameIgnoreCaseAndDeletedAtIsNull(name);
    }

    @Override
    public PagedResult<Community> findPublicCommunities(String search, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<CommunityJpaEntity> entities = springDataCommunityRepository.findByVisibilityAndSearch(
                CommunityVisibility.PUBLIC,
                search != null && !search.isBlank() ? search.trim() : null,
                pageRequest
        );

        return new PagedResult<>(
                entities.map(CommunityJpaEntity::toDomain).getContent(),
                entities.getNumber(),
                entities.getSize(),
                entities.getTotalElements(),
                entities.getTotalPages(),
                entities.hasNext()
        );
    }

    @Override
    public java.util.List<Community> findAllById(Iterable<UUID> ids) {
        return springDataCommunityRepository.findAllById(ids)
                .stream()
                .map(CommunityJpaEntity::toDomain)
                .toList();
    }

    @Override
    public long countByOwnerId(UUID ownerId) {
        return springDataCommunityRepository.countByOwnerIdAndDeletedAtIsNull(ownerId);
    }
}
