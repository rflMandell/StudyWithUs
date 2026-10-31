package com.studywithus.community.presentation;

import com.studywithus.community.application.CommunityJoinRequestResult;
import com.studywithus.community.application.CommunityMemberResult;
import com.studywithus.community.application.CommunityResult;
import com.studywithus.community.application.CreateCommunityUseCase;
import com.studywithus.community.application.GetCommunityDetailsUseCase;
import com.studywithus.community.application.JoinCommunityCommand;
import com.studywithus.community.application.JoinCommunityUseCase;
import com.studywithus.community.application.LeaveCommunityCommand;
import com.studywithus.community.application.LeaveCommunityUseCase;
import com.studywithus.community.application.ListCommunitiesUseCase;
import com.studywithus.community.application.ListCommunityMembersUseCase;
import com.studywithus.community.application.ListJoinRequestsQuery;
import com.studywithus.community.application.ListJoinRequestsUseCase;
import com.studywithus.community.application.RemoveCommunityMemberCommand;
import com.studywithus.community.application.RemoveCommunityMemberUseCase;
import com.studywithus.community.application.RequestJoinCommunityCommand;
import com.studywithus.community.application.RequestJoinCommunityUseCase;
import com.studywithus.community.application.ReviewJoinRequestCommand;
import com.studywithus.community.application.ReviewJoinRequestUseCase;
import com.studywithus.community.application.UpdateMemberRoleCommand;
import com.studywithus.community.application.UpdateMemberRoleUseCase;
import com.studywithus.community.domain.CommunityJoinRequestStatus;
import com.studywithus.community.domain.CommunityMember;
import com.studywithus.identity.infrastructure.SecurityUserPrincipal;
import com.studywithus.shared.domain.PagedResult;
import com.studywithus.shared.presentation.PagedResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/communities")
@RequiredArgsConstructor
public class CommunityController {

    private final CreateCommunityUseCase createCommunityUseCase;
    private final GetCommunityDetailsUseCase getCommunityDetailsUseCase;
    private final ListCommunitiesUseCase listCommunitiesUseCase;
    private final JoinCommunityUseCase joinCommunityUseCase;
    private final LeaveCommunityUseCase leaveCommunityUseCase;
    private final ListCommunityMembersUseCase listCommunityMembersUseCase;
    private final RequestJoinCommunityUseCase requestJoinCommunityUseCase;
    private final ListJoinRequestsUseCase listJoinRequestsUseCase;
    private final ReviewJoinRequestUseCase reviewJoinRequestUseCase;
    private final UpdateMemberRoleUseCase updateMemberRoleUseCase;
    private final RemoveCommunityMemberUseCase removeCommunityMemberUseCase;

    @PostMapping
    public ResponseEntity<CommunityResponse> createCommunity(
            @Valid @RequestBody CreateCommunityRequest request,
            @AuthenticationPrincipal SecurityUserPrincipal principal
    ) {
        CommunityResult result = createCommunityUseCase.execute(request.toCommand(principal.getId()));
        return ResponseEntity.status(HttpStatus.CREATED).body(CommunityResponse.from(result));
    }

    @GetMapping
    public ResponseEntity<PagedResponse<CommunityResponse>> listCommunities(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal SecurityUserPrincipal principal
    ) {
        UUID userId = principal != null ? principal.getId() : null;
        PagedResult<CommunityResult> pagedResult = listCommunitiesUseCase.execute(search, page, size, userId);
        return ResponseEntity.ok(PagedResponse.from(pagedResult, CommunityResponse::from));
    }

    @GetMapping("/{communityId}")
    public ResponseEntity<CommunityResponse> getCommunityDetails(
            @PathVariable UUID communityId,
            @AuthenticationPrincipal SecurityUserPrincipal principal
    ) {
        UUID userId = principal != null ? principal.getId() : null;
        CommunityResult result = getCommunityDetailsUseCase.execute(communityId, userId);
        return ResponseEntity.ok(CommunityResponse.from(result));
    }

    @PostMapping("/{communityId}/members")
    public ResponseEntity<CommunityMemberResponse> joinCommunity(
            @PathVariable UUID communityId,
            @AuthenticationPrincipal SecurityUserPrincipal principal
    ) {
        CommunityMember member = joinCommunityUseCase.execute(new JoinCommunityCommand(communityId, principal.getId()));
        CommunityMemberResult result = new CommunityMemberResult(
                member.getCommunityId(),
                member.getUserId(),
                principal.getUsername(),
                principal.getUsername(),
                null,
                member.getRole(),
                member.getStatus(),
                member.getJoinedAt()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(CommunityMemberResponse.from(result));
    }

    @DeleteMapping("/{communityId}/members/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leaveCommunity(
            @PathVariable UUID communityId,
            @AuthenticationPrincipal SecurityUserPrincipal principal
    ) {
        leaveCommunityUseCase.execute(new LeaveCommunityCommand(communityId, principal.getId()));
    }

    @GetMapping("/{communityId}/members")
    public ResponseEntity<PagedResponse<CommunityMemberResponse>> listMembers(
            @PathVariable UUID communityId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PagedResult<CommunityMemberResult> pagedResult = listCommunityMembersUseCase.execute(communityId, page, size);
        return ResponseEntity.ok(PagedResponse.from(pagedResult, CommunityMemberResponse::from));
    }

    @PostMapping("/{communityId}/join-requests")
    public ResponseEntity<CommunityJoinRequestResponse> requestJoin(
            @PathVariable UUID communityId,
            @AuthenticationPrincipal SecurityUserPrincipal principal
    ) {
        CommunityJoinRequestResult result = requestJoinCommunityUseCase.execute(
                new RequestJoinCommunityCommand(communityId, principal.getId())
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(CommunityJoinRequestResponse.from(result));
    }

    @GetMapping("/{communityId}/join-requests")
    public ResponseEntity<PagedResponse<CommunityJoinRequestResponse>> listJoinRequests(
            @PathVariable UUID communityId,
            @RequestParam(defaultValue = "PENDING") CommunityJoinRequestStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal SecurityUserPrincipal principal
    ) {
        PagedResult<CommunityJoinRequestResult> pagedResult = listJoinRequestsUseCase.execute(
                new ListJoinRequestsQuery(communityId, principal.getId(), status, page, size)
        );
        return ResponseEntity.ok(PagedResponse.from(pagedResult, CommunityJoinRequestResponse::from));
    }

    @PostMapping("/{communityId}/join-requests/{requestId}/approve")
    public ResponseEntity<CommunityJoinRequestResponse> approveJoinRequest(
            @PathVariable UUID communityId,
            @PathVariable UUID requestId,
            @AuthenticationPrincipal SecurityUserPrincipal principal
    ) {
        CommunityJoinRequestResult result = reviewJoinRequestUseCase.execute(
                new ReviewJoinRequestCommand(communityId, requestId, principal.getId(), true)
        );
        return ResponseEntity.ok(CommunityJoinRequestResponse.from(result));
    }

    @PostMapping("/{communityId}/join-requests/{requestId}/reject")
    public ResponseEntity<CommunityJoinRequestResponse> rejectJoinRequest(
            @PathVariable UUID communityId,
            @PathVariable UUID requestId,
            @AuthenticationPrincipal SecurityUserPrincipal principal
    ) {
        CommunityJoinRequestResult result = reviewJoinRequestUseCase.execute(
                new ReviewJoinRequestCommand(communityId, requestId, principal.getId(), false)
        );
        return ResponseEntity.ok(CommunityJoinRequestResponse.from(result));
    }

    @PutMapping("/{communityId}/members/{userId}/role")
    public ResponseEntity<CommunityMemberResponse> updateMemberRole(
            @PathVariable UUID communityId,
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateMemberRoleRequest request,
            @AuthenticationPrincipal SecurityUserPrincipal principal
    ) {
        CommunityMemberResult result = updateMemberRoleUseCase.execute(
                new UpdateMemberRoleCommand(communityId, userId, principal.getId(), request.role())
        );
        return ResponseEntity.ok(CommunityMemberResponse.from(result));
    }

    @DeleteMapping("/{communityId}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(
            @PathVariable UUID communityId,
            @PathVariable UUID userId,
            @AuthenticationPrincipal SecurityUserPrincipal principal
    ) {
        removeCommunityMemberUseCase.execute(
                new RemoveCommunityMemberCommand(communityId, userId, principal.getId())
        );
    }
}
