package com.studywithus.gamification.presentation;

import com.studywithus.gamification.application.GetUserStreakUseCase;
import com.studywithus.identity.infrastructure.SecurityUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me/streak")
@RequiredArgsConstructor
public class StreakController {

    private final GetUserStreakUseCase getUserStreakUseCase;

    @GetMapping
    public ResponseEntity<UserStreakResponse> getMyStreak(@AuthenticationPrincipal SecurityUserPrincipal principal) {
        return ResponseEntity.ok(UserStreakResponse.from(getUserStreakUseCase.execute(principal.getId())));
    }
}
