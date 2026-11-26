package com.studywithus.social.presentation;

import com.studywithus.checkin.application.LikeToggleResult;
import com.studywithus.checkin.presentation.LikeToggleResponse;
import com.studywithus.identity.infrastructure.SecurityUserPrincipal;
import com.studywithus.shared.domain.PagedResult;
import com.studywithus.shared.presentation.PagedResponse;
import com.studywithus.social.application.CommentResult;
import com.studywithus.social.application.CreateCommentCommand;
import com.studywithus.social.application.CreateCommentUseCase;
import com.studywithus.social.application.DeleteCommentCommand;
import com.studywithus.social.application.DeleteCommentUseCase;
import com.studywithus.social.application.ListCommentsQuery;
import com.studywithus.social.application.ListCommentsUseCase;
import com.studywithus.social.application.ToggleCommentLikeCommand;
import com.studywithus.social.application.ToggleCommentLikeUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CommentController {

    private final CreateCommentUseCase createCommentUseCase;
    private final ListCommentsUseCase listCommentsUseCase;
    private final DeleteCommentUseCase deleteCommentUseCase;
    private final ToggleCommentLikeUseCase toggleCommentLikeUseCase;

    @PostMapping("/checkins/{checkinId}/comments")
    public ResponseEntity<CommentResponse> createComment(
            @PathVariable UUID checkinId,
            @Valid @RequestBody CreateCommentRequest request,
            @AuthenticationPrincipal SecurityUserPrincipal principal
    ) {
        CommentResult result = createCommentUseCase.execute(
                new CreateCommentCommand(checkinId, principal.getId(), request.content(), request.parentCommentId())
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(CommentResponse.from(result));
    }

    @GetMapping("/checkins/{checkinId}/comments")
    public ResponseEntity<PagedResponse<CommentResponse>> listComments(
            @PathVariable UUID checkinId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal SecurityUserPrincipal principal
    ) {
        UUID callerId = principal != null ? principal.getId() : null;
        PagedResult<CommentResult> pagedResult = listCommentsUseCase.execute(
                new ListCommentsQuery(checkinId, callerId, page, size)
        );
        return ResponseEntity.ok(PagedResponse.from(pagedResult, CommentResponse::from));
    }

    @PostMapping("/comments/{commentId}/like")
    public ResponseEntity<LikeToggleResponse> toggleLike(
            @PathVariable UUID commentId,
            @AuthenticationPrincipal SecurityUserPrincipal principal
    ) {
        LikeToggleResult result = toggleCommentLikeUseCase.execute(
                new ToggleCommentLikeCommand(commentId, principal.getId())
        );
        return ResponseEntity.ok(LikeToggleResponse.from(result));
    }

    @DeleteMapping("/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(
            @PathVariable UUID commentId,
            @AuthenticationPrincipal SecurityUserPrincipal principal
    ) {
        deleteCommentUseCase.execute(new DeleteCommentCommand(commentId, principal.getId()));
    }
}
