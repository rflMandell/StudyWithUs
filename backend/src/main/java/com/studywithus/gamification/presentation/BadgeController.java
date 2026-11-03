package com.studywithus.gamification.presentation;

import com.studywithus.gamification.application.GetUserBadgesUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/{userId}/badges")
@RequiredArgsConstructor
public class BadgeController {

    private final GetUserBadgesUseCase getUserBadgesUseCase;

    @GetMapping
    public ResponseEntity<List<UserBadgeResponse>> getUserBadges(@PathVariable UUID userId) {
        return ResponseEntity.ok(getUserBadgesUseCase.execute(userId).stream()
                .map(UserBadgeResponse::from)
                .toList());
    }
}
