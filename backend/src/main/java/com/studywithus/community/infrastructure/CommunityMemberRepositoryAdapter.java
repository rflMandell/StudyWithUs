package com.studywithus.community.infrastructure;

import com.studywithus.community.domain.CommunityMember;
import com.studywithus.community.domain.CommunityMemberRepository;
import com.studywithus.community.domain.MemberStatus;
import com.studywithus.shared.domain.PagedResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

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
}
