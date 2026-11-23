package com.studywithus.checkin.application;

import com.studywithus.checkin.domain.Checkin;
import com.studywithus.checkin.domain.CheckinRepository;
import com.studywithus.community.domain.Community;
import com.studywithus.community.domain.CommunityMemberRepository;
import com.studywithus.community.domain.CommunityRepository;
import com.studywithus.shared.domain.DomainException;
import com.studywithus.shared.domain.PagedResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ListCommunityCheckinsUseCase {

    private final CheckinRepository checkinRepository;
    private final CommunityRepository communityRepository;
    private final CommunityMemberRepository communityMemberRepository;

    @Transactional(readOnly = true)
    public PagedResult<CheckinResult> execute(UUID communityId, int page, int size) {
        return execute(communityId, page, size, null);
    }

    @Transactional(readOnly = true)
    public PagedResult<CheckinResult> execute(UUID communityId, int page, int size, UUID callerId) {
        Community community = communityRepository.findById(communityId)
                .filter(Community::isActive)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "COMMUNITY_NOT_FOUND", "Community not found"));

        if (community.isPrivate()) {
            if (callerId == null || !communityMemberRepository.isUserActiveMember(communityId, callerId)) {
                throw new DomainException(
                        HttpStatus.FORBIDDEN,
                        "COMMUNITY_ACCESS_DENIED",
                        "Only active members can view check-ins of a private community"
                );
            }
        }

        PagedResult<Checkin> pagedResult = checkinRepository.findByCommunityIdActive(communityId, page, size);

        List<CheckinResult> content = pagedResult.content().stream()
                .map(CheckinResult::fromDomain)
                .toList();

        return new PagedResult<>(
                content,
                pagedResult.page(),
                pagedResult.size(),
                pagedResult.totalElements(),
                pagedResult.totalPages(),
                pagedResult.hasNext()
        );
    }
}
