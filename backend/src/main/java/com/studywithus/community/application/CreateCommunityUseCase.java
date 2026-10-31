package com.studywithus.community.application;

import com.studywithus.community.domain.Community;
import com.studywithus.community.domain.CommunityMember;
import com.studywithus.community.domain.CommunityMemberRepository;
import com.studywithus.community.domain.CommunityRepository;
import com.studywithus.community.domain.CommunityRole;
import com.studywithus.community.domain.MemberStatus;
import com.studywithus.shared.application.UseCase;
import com.studywithus.shared.domain.DomainException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class CreateCommunityUseCase implements UseCase<CreateCommunityCommand, CommunityResult> {

    private final CommunityRepository communityRepository;
    private final CommunityMemberRepository communityMemberRepository;

    @Override
    @Transactional
    public CommunityResult execute(CreateCommunityCommand command) {
        String cleanName = command.name().trim();

        if (communityRepository.existsByNameIgnoreCase(cleanName)) {
            throw new DomainException(HttpStatus.CONFLICT, "COMMUNITY_NAME_ALREADY_EXISTS", "A community with this name already exists");
        }

        Instant now = Instant.now();
        Community community = Community.builder()
                .name(cleanName)
                .description(command.description())
                .imageUrl(command.imageUrl())
                .visibility(command.visibility())
                .ownerId(command.ownerId())
                .rankingType("WEEKLY")
                .timezone(command.timezone() != null && !command.timezone().isBlank() ? command.timezone() : "UTC")
                .createdAt(now)
                .updatedAt(now)
                .build();

        Community savedCommunity = communityRepository.save(community);

        CommunityMember ownerMember = CommunityMember.builder()
                .communityId(savedCommunity.getId())
                .userId(command.ownerId())
                .role(CommunityRole.OWNER)
                .status(MemberStatus.ACTIVE)
                .joinedAt(now)
                .build();

        communityMemberRepository.save(ownerMember);

        return CommunityResult.from(savedCommunity, 1, CommunityRole.OWNER);
    }
}
