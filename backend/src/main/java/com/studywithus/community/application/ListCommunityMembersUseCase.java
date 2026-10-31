package com.studywithus.community.application;

import com.studywithus.community.domain.Community;
import com.studywithus.community.domain.CommunityMember;
import com.studywithus.community.domain.CommunityMemberRepository;
import com.studywithus.community.domain.CommunityRepository;
import com.studywithus.identity.domain.User;
import com.studywithus.identity.domain.UserRepository;
import com.studywithus.shared.domain.PagedResult;
import com.studywithus.shared.domain.DomainException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ListCommunityMembersUseCase {

    private final CommunityRepository communityRepository;
    private final CommunityMemberRepository communityMemberRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public PagedResult<CommunityMemberResult> execute(UUID communityId, int page, int size) {
        communityRepository.findById(communityId)
                .filter(Community::isActive)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "COMMUNITY_NOT_FOUND", "Community not found"));

        PagedResult<CommunityMember> pagedMembers = communityMemberRepository.findActiveMembers(communityId, page, size);

        List<CommunityMemberResult> results = pagedMembers.content().stream()
                .map(member -> {
                    User user = userRepository.findById(member.getUserId()).orElse(null);
                    String username = user != null ? user.getUsername() : "unknown";
                    String name = user != null ? user.getName() : "Unknown";
                    String avatarUrl = user != null ? user.getAvatarUrl() : null;

                    return new CommunityMemberResult(
                            member.getCommunityId(),
                            member.getUserId(),
                            username,
                            name,
                            avatarUrl,
                            member.getRole(),
                            member.getStatus(),
                            member.getJoinedAt()
                    );
                })
                .toList();

        return new PagedResult<>(
                results,
                pagedMembers.page(),
                pagedMembers.size(),
                pagedMembers.totalElements(),
                pagedMembers.totalPages(),
                pagedMembers.hasNext()
        );
    }
}
