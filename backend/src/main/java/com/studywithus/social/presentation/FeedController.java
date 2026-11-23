package com.studywithus.social.presentation;

import com.studywithus.identity.infrastructure.SecurityUserPrincipal;
import com.studywithus.media.domain.ObjectStoragePort;
import com.studywithus.social.application.FeedCursorResult;
import com.studywithus.social.application.GetCommunityFeedUseCase;
import com.studywithus.social.application.GetUserFeedQuery;
import com.studywithus.social.application.GetUserFeedUseCase;
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
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class FeedController {

    private final GetUserFeedUseCase getUserFeedUseCase;
    private final GetCommunityFeedUseCase getCommunityFeedUseCase;
    private final ObjectStoragePort objectStoragePort;

    @GetMapping("/feed")
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

        return ResponseEntity.ok(FeedResponse.from(result, objectStoragePort::getPublicUrl));
    }

    @GetMapping("/communities/{communityId}/feed")
    public ResponseEntity<FeedResponse> getCommunityFeed(
            @PathVariable UUID communityId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit,
            @AuthenticationPrincipal SecurityUserPrincipal principal
    ) {
        UUID callerId = principal != null ? principal.getId() : null;
        FeedCursorResult result = getCommunityFeedUseCase.execute(communityId, callerId, cursor, limit);
        return ResponseEntity.ok(FeedResponse.from(result, objectStoragePort::getPublicUrl));
    }
}
