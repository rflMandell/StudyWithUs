package com.studywithus.social.presentation;

import com.studywithus.identity.infrastructure.SecurityUserPrincipal;
import com.studywithus.social.application.FollowActionResult;
import com.studywithus.social.application.FollowStatusResult;
import com.studywithus.social.application.FollowUserUseCase;
import com.studywithus.social.application.GetFollowStatusUseCase;
import com.studywithus.social.application.ListPendingFollowRequestsUseCase;
import com.studywithus.social.application.RespondFollowRequestUseCase;
import com.studywithus.social.application.UnfollowUserUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/follows")
@RequiredArgsConstructor
public class FollowController {

    private final FollowUserUseCase followUserUseCase;
    private final UnfollowUserUseCase unfollowUserUseCase;
    private final GetFollowStatusUseCase getFollowStatusUseCase;
    private final ListPendingFollowRequestsUseCase listPendingFollowRequestsUseCase;
    private final RespondFollowRequestUseCase respondFollowRequestUseCase;

    @PostMapping("/{targetUserId}")
    public ResponseEntity<FollowActionResponse> followUser(
            @PathVariable UUID targetUserId,
            @AuthenticationPrincipal SecurityUserPrincipal principal
    ) {
        FollowActionResult result = followUserUseCase.execute(principal.getId(), targetUserId);
        return ResponseEntity.ok(FollowActionResponse.from(result));
    }

    @DeleteMapping("/{targetUserId}")
    public ResponseEntity<FollowActionResponse> unfollowUser(
            @PathVariable UUID targetUserId,
            @AuthenticationPrincipal SecurityUserPrincipal principal
    ) {
        FollowActionResult result = unfollowUserUseCase.execute(principal.getId(), targetUserId);
        return ResponseEntity.ok(FollowActionResponse.from(result));
    }

    @GetMapping("/{targetUserId}/status")
    public ResponseEntity<FollowStatusResponse> getFollowStatus(
            @PathVariable UUID targetUserId,
            @AuthenticationPrincipal SecurityUserPrincipal principal
    ) {
        FollowStatusResult result = getFollowStatusUseCase.execute(principal.getId(), targetUserId);
        return ResponseEntity.ok(FollowStatusResponse.from(result));
    }

    @GetMapping("/requests/pending")
    public ResponseEntity<List<PendingFollowRequestResponse>> listPendingRequests(
            @AuthenticationPrincipal SecurityUserPrincipal principal
    ) {
        List<PendingFollowRequestResponse> response = listPendingFollowRequestsUseCase.execute(principal.getId())
                .stream()
                .map(PendingFollowRequestResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/requests/{requestId}/accept")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void acceptFollowRequest(
            @PathVariable UUID requestId,
            @AuthenticationPrincipal SecurityUserPrincipal principal
    ) {
        respondFollowRequestUseCase.execute(requestId, principal.getId(), true);
    }

    @PostMapping("/requests/{requestId}/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void rejectFollowRequest(
            @PathVariable UUID requestId,
            @AuthenticationPrincipal SecurityUserPrincipal principal
    ) {
        respondFollowRequestUseCase.execute(requestId, principal.getId(), false);
    }
}
