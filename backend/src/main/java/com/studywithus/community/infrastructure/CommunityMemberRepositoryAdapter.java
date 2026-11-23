package com.studywithus.community.infrastructure;

import com.studywithus.community.domain.CommunityMember;
import com.studywithus.community.domain.CommunityMemberRepository;
import com.studywithus.community.domain.MemberStatus;
import com.studywithus.shared.domain.PagedResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CommunityMemberRepositoryAdapter implements CommunityMemberRepository {

    private final SpringDataCommunityMemberRepository springDataCommunityMemberRepository;

    @Override
    public CommunityMember save(CommunityMember member) {
        CommunityMemberJpaEntity entity = CommunityMemberJpaEntity.fromDomain(member);
        return springDataCommunityMemberRepository.save(entity).toDomain();
    }

    @Override
    public Optional<CommunityMember> findByCommunityIdAndUserId(UUID communityId, UUID userId) {
        return springDataCommunityMemberRepository.findByCommunityIdAndUserId(communityId, userId)
                .map(CommunityMemberJpaEntity::toDomain);
    }

    @Override
    public boolean isUserActiveMember(UUID communityId, UUID userId) {
        return springDataCommunityMemberRepository.isUserActiveMember(communityId, userId);
    }

    @Override
    public long countActiveMembers(UUID communityId) {
        return springDataCommunityMemberRepository.countActiveMembers(communityId);
    }

    @Override
    public PagedResult<CommunityMember> findActiveMembers(UUID communityId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<CommunityMemberJpaEntity> entities = springDataCommunityMemberRepository.findByCommunityIdAndStatus(
                communityId,
                MemberStatus.ACTIVE,
                pageRequest
        );

        return new PagedResult<>(
                entities.map(CommunityMemberJpaEntity::toDomain).getContent(),
                entities.getNumber(),
                entities.getSize(),
                entities.getTotalElements(),
                entities.getTotalPages(),
                entities.hasNext()
        );
    }

    @Override
    public java.util.List<UUID> findActiveCommunityIdsByUserId(UUID userId) {
        return springDataCommunityMemberRepository.findActiveCommunityIdsByUserId(userId);
    }

    @Override
    public java.util.List<UUID> findActiveUserIdsByCommunityId(UUID communityId) {
        return springDataCommunityMemberRepository.findActiveUserIdsByCommunityId(communityId);
    }

    @Override
    public Map<UUID, Long> countActiveMembersByCommunityIds(Collection<UUID> communityIds) {
        if (communityIds == null || communityIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, Long> counts = new HashMap<>();
        for (Object[] row : springDataCommunityMemberRepository.countActiveMembersByCommunityIds(communityIds)) {
            counts.put((UUID) row[0], ((Number) row[1]).longValue());
        }
        return counts;
    }

    @Override
    public List<CommunityMember> findActiveByCommunityIdsAndUserId(Collection<UUID> communityIds, UUID userId) {
        if (communityIds == null || communityIds.isEmpty()) {
            return List.of();
        }
        return springDataCommunityMemberRepository.findActiveByCommunityIdsAndUserId(communityIds, userId)
                .stream()
                .map(CommunityMemberJpaEntity::toDomain)
                .toList();
    }
}
