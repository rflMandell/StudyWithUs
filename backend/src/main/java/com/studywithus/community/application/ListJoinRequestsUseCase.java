package com.studywithus.community.application;

import com.studywithus.community.domain.CommunityJoinRequest;
import com.studywithus.community.domain.CommunityJoinRequestRepository;
import com.studywithus.community.domain.CommunityMember;
import com.studywithus.community.domain.CommunityMemberRepository;
import com.studywithus.shared.application.UseCase;
import com.studywithus.shared.domain.DomainException;
import com.studywithus.shared.domain.PagedResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListJoinRequestsUseCase implements UseCase<ListJoinRequestsQuery, PagedResult<CommunityJoinRequestResult>> {

    private final CommunityMemberRepository communityMemberRepository;
    private final CommunityJoinRequestRepository joinRequestRepository;

    @Override
    @Transactional(readOnly = true)
    public PagedResult<CommunityJoinRequestResult> execute(ListJoinRequestsQuery query) {
        CommunityMember member = communityMemberRepository.findByCommunityIdAndUserId(
                query.communityId(),
                query.requesterUserId()
        ).orElseThrow(() -> new DomainException(HttpStatus.FORBIDDEN, "COMMUNITY_ACCESS_DENIED", "User is not a member of this community"));

        if (!member.isAdmin()) {
            throw new DomainException(HttpStatus.FORBIDDEN, "COMMUNITY_ACCESS_DENIED", "Only community admins or owner can view join requests");
        }

        PagedResult<CommunityJoinRequest> paged = joinRequestRepository.findByCommunityIdAndStatus(
                query.communityId(),
                query.status(),
                query.page(),
                query.size()
        );

        List<CommunityJoinRequestResult> items = paged.content().stream()
                .map(CommunityJoinRequestResult::fromDomain)
                .toList();

        return new PagedResult<>(
                items,
                paged.page(),
                paged.size(),
                paged.totalElements(),
                paged.totalPages(),
                paged.hasNext()
        );
    }
}
