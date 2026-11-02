package com.studywithus.gamification.presentation;

import com.studywithus.gamification.application.GetUserPointsSummaryUseCase;
import com.studywithus.gamification.application.ListUserPointsHistoryUseCase;
import com.studywithus.gamification.application.PointTransactionResult;
import com.studywithus.gamification.application.UserPointsSummaryResult;
import com.studywithus.identity.infrastructure.SecurityUserPrincipal;
import com.studywithus.shared.domain.PagedResult;
import com.studywithus.shared.presentation.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me/points")
@RequiredArgsConstructor
public class PointsController {

    private final GetUserPointsSummaryUseCase getUserPointsSummaryUseCase;
    private final ListUserPointsHistoryUseCase listUserPointsHistoryUseCase;

    @GetMapping
    public ResponseEntity<UserPointsResponse> getPointsSummary(
            @AuthenticationPrincipal SecurityUserPrincipal principal
    ) {
        UserPointsSummaryResult result = getUserPointsSummaryUseCase.execute(principal.getId());
        return ResponseEntity.ok(UserPointsResponse.from(result));
    }

    @GetMapping("/history")
    public ResponseEntity<PagedResponse<PointTransactionResponse>> getPointsHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal SecurityUserPrincipal principal
    ) {
        PagedResult<PointTransactionResult> pagedResult = listUserPointsHistoryUseCase.execute(
                principal.getId(),
                page,
                size
        );
        return ResponseEntity.ok(PagedResponse.from(pagedResult, PointTransactionResponse::from));
    }
}
