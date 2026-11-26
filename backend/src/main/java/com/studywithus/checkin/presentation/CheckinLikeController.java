package com.studywithus.checkin.presentation;

import com.studywithus.checkin.application.LikeToggleResult;
import com.studywithus.checkin.application.ToggleCheckinLikeCommand;
import com.studywithus.checkin.application.ToggleCheckinLikeUseCase;
import com.studywithus.identity.infrastructure.SecurityUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CheckinLikeController {

    private final ToggleCheckinLikeUseCase toggleCheckinLikeUseCase;

    @PostMapping("/checkins/{checkinId}/like")
    public ResponseEntity<LikeToggleResponse> toggleLike(
            @PathVariable UUID checkinId,
            @AuthenticationPrincipal SecurityUserPrincipal principal
    ) {
        LikeToggleResult result = toggleCheckinLikeUseCase.execute(
                new ToggleCheckinLikeCommand(checkinId, principal.getId())
        );
        return ResponseEntity.ok(LikeToggleResponse.from(result));
    }
}
