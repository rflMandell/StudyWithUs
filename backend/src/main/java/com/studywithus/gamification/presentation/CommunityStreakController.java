package com.studywithus.gamification.presentation;

import com.studywithus.gamification.application.GetCommunityStreakUseCase;
import com.studywithus.gamification.application.ManageCommunityFreeDaysUseCase;
import com.studywithus.identity.infrastructure.SecurityUserPrincipal;
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
@RequestMapping("/api/v1/communities/{communityId}")
@RequiredArgsConstructor
public class CommunityStreakController {

    private final GetCommunityStreakUseCase getCommunityStreakUseCase;
    private final ManageCommunityFreeDaysUseCase manageCommunityFreeDaysUseCase;

    @GetMapping("/streaks/me")
    public ResponseEntity<CommunityStreakResponse> getMyCommunityStreak(
            @PathVariable UUID communityId,
            @AuthenticationPrincipal SecurityUserPrincipal principal
    ) {
        var result = getCommunityStreakUseCase.execute(communityId, principal.getId());
        return ResponseEntity.ok(CommunityStreakResponse.from(result));
    }

    @GetMapping("/free-days")
    public ResponseEntity<List<Integer>> getFreeDays(@PathVariable UUID communityId) {
        return ResponseEntity.ok(manageCommunityFreeDaysUseCase.getFreeDays(communityId));
    }

    @PostMapping("/free-days/{dayOfWeek}")
    @ResponseStatus(HttpStatus.CREATED)
    public void addFreeDay(
            @PathVariable UUID communityId,
            @PathVariable int dayOfWeek,
            @AuthenticationPrincipal SecurityUserPrincipal principal
    ) {
        manageCommunityFreeDaysUseCase.addFreeDay(communityId, dayOfWeek, principal.getId());
    }

    @DeleteMapping("/free-days/{dayOfWeek}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeFreeDay(
            @PathVariable UUID communityId,
            @PathVariable int dayOfWeek,
            @AuthenticationPrincipal SecurityUserPrincipal principal
    ) {
        manageCommunityFreeDaysUseCase.removeFreeDay(communityId, dayOfWeek, principal.getId());
    }
}
