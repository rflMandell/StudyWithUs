package com.studywithus.social.presentation;

import com.studywithus.identity.infrastructure.SecurityUserPrincipal;
import com.studywithus.social.application.FeedCursorResult;
import com.studywithus.social.application.GetUserFeedQuery;
import com.studywithus.social.application.GetUserFeedUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/feed")
@RequiredArgsConstructor
public class FeedController {

    private final GetUserFeedUseCase getUserFeedUseCase;

    @GetMapping
    public ResponseEntity<FeedResponse> getFeed(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit,
            @AuthenticationPrincipal SecurityUserPrincipal principal
    ) {
        FeedCursorResult result = getUserFeedUseCase.execute(new GetUserFeedQuery(
                principal.getId(),
                cursor,
                limit
        ));

        return ResponseEntity.ok(FeedResponse.from(result));
    }
}
