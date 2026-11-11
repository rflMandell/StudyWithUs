package com.studywithus.challenge.presentation;

import com.studywithus.challenge.application.ChallengeParticipantResult;
import com.studywithus.challenge.application.ChallengeResult;
import com.studywithus.challenge.application.CreateChallengeUseCase;
import com.studywithus.challenge.application.GetChallengeProgressQuery;
import com.studywithus.challenge.application.GetChallengeProgressUseCase;
import com.studywithus.challenge.application.JoinChallengeCommand;
import com.studywithus.challenge.application.JoinChallengeUseCase;
import com.studywithus.challenge.application.ListCommunityChallengesUseCase;
import com.studywithus.identity.infrastructure.SecurityUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ChallengeController {

    private final CreateChallengeUseCase createChallengeUseCase;
    private final ListCommunityChallengesUseCase listCommunityChallengesUseCase;
    private final JoinChallengeUseCase joinChallengeUseCase;
    private final GetChallengeProgressUseCase getChallengeProgressUseCase;

    @PostMapping("/communities/{communityId}/challenges")
    public ResponseEntity<ChallengeResponse> createCommunityChallenge(
            @PathVariable UUID communityId,
            @Valid @RequestBody CreateChallengeRequest request,
            @AuthenticationPrincipal SecurityUserPrincipal principal
    ) {
        ChallengeResult result = createChallengeUseCase.execute(
                request.toCommand(principal.getId(), communityId)
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ChallengeResponse.from(result));
    }

    @GetMapping("/communities/{communityId}/challenges")
    public ResponseEntity<List<ChallengeResponse>> listCommunityChallenges(
            @PathVariable UUID communityId
    ) {
        List<ChallengeResult> results = listCommunityChallengesUseCase.execute(communityId);
        List<ChallengeResponse> responses = results.stream()
                .map(ChallengeResponse::from)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/challenges/{challengeId}/join")
    public ResponseEntity<ChallengeParticipantResponse> joinChallenge(
            @PathVariable UUID challengeId,
            @AuthenticationPrincipal SecurityUserPrincipal principal
    ) {
        ChallengeParticipantResult result = joinChallengeUseCase.execute(
                new JoinChallengeCommand(challengeId, principal.getId())
        );
        return ResponseEntity.ok(ChallengeParticipantResponse.from(result));
    }

    @GetMapping("/challenges/{challengeId}/progress/me")
    public ResponseEntity<ChallengeParticipantResponse> getMyProgress(
            @PathVariable UUID challengeId,
            @AuthenticationPrincipal SecurityUserPrincipal principal
    ) {
        ChallengeParticipantResult result = getChallengeProgressUseCase.execute(
                new GetChallengeProgressQuery(challengeId, principal.getId())
        );
        return ResponseEntity.ok(ChallengeParticipantResponse.from(result));
    }
}
