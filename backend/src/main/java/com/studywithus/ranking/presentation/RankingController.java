package com.studywithus.ranking.presentation;

import com.studywithus.identity.infrastructure.SecurityUserPrincipal;
import com.studywithus.ranking.application.GetCommunityRankingUseCase;
import com.studywithus.ranking.application.GetUserCommunityRankingPositionUseCase;
import com.studywithus.ranking.application.RankingEntryResult;
import com.studywithus.ranking.application.UserRankingPositionResult;
import com.studywithus.shared.domain.PagedResult;
import com.studywithus.shared.presentation.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/communities/{communityId}/ranking")
@RequiredArgsConstructor
public class RankingController {

    private final GetCommunityRankingUseCase getCommunityRankingUseCase;
    private final GetUserCommunityRankingPositionUseCase getUserCommunityRankingPositionUseCase;

    @GetMapping
    public ResponseEntity<PagedResponse<RankingEntryResponse>> getCommunityRanking(
            @PathVariable UUID communityId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PagedResult<RankingEntryResult> pagedResult = getCommunityRankingUseCase.execute(communityId, page, size);
        return ResponseEntity.ok(PagedResponse.from(pagedResult, RankingEntryResponse::from));
    }

    @GetMapping("/me")
    public ResponseEntity<UserRankingPositionResponse> getMyRankingPosition(
            @PathVariable UUID communityId,
            @AuthenticationPrincipal SecurityUserPrincipal principal
    ) {
        UserRankingPositionResult result = getUserCommunityRankingPositionUseCase.execute(communityId, principal.getId());
        return ResponseEntity.ok(UserRankingPositionResponse.from(result));
    }
}
