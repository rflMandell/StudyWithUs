package com.studywithus.checkin.presentation;

import com.studywithus.checkin.application.CheckinResult;
import com.studywithus.checkin.application.CreateCheckinUseCase;
import com.studywithus.checkin.application.DeleteCheckinCommand;
import com.studywithus.checkin.application.DeleteCheckinUseCase;
import com.studywithus.checkin.application.GetCheckinDetailsUseCase;
import com.studywithus.checkin.application.ListCommunityCheckinsUseCase;
import com.studywithus.identity.infrastructure.SecurityUserPrincipal;
import com.studywithus.shared.domain.PagedResult;
import com.studywithus.shared.presentation.PagedResponse;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CheckinController {

    private final CreateCheckinUseCase createCheckinUseCase;
    private final GetCheckinDetailsUseCase getCheckinDetailsUseCase;
    private final ListCommunityCheckinsUseCase listCommunityCheckinsUseCase;
    private final DeleteCheckinUseCase deleteCheckinUseCase;

    @PostMapping("/checkins")
    public ResponseEntity<CheckinResponse> createCheckin(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateCheckinRequest request,
            @AuthenticationPrincipal SecurityUserPrincipal principal
    ) {
        CheckinResult result = createCheckinUseCase.execute(request.toCommand(principal.getId(), idempotencyKey));
        return ResponseEntity.status(HttpStatus.CREATED).body(CheckinResponse.from(result));
    }

    @GetMapping("/checkins/{checkinId}")
    public ResponseEntity<CheckinResponse> getCheckinDetails(@PathVariable UUID checkinId) {
        CheckinResult result = getCheckinDetailsUseCase.execute(checkinId);
        return ResponseEntity.ok(CheckinResponse.from(result));
    }

    @GetMapping("/communities/{communityId}/checkins")
    public ResponseEntity<PagedResponse<CheckinResponse>> listCommunityCheckins(
            @PathVariable UUID communityId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PagedResult<CheckinResult> pagedResult = listCommunityCheckinsUseCase.execute(communityId, page, size);
        return ResponseEntity.ok(PagedResponse.from(pagedResult, CheckinResponse::from));
    }

    @DeleteMapping("/checkins/{checkinId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCheckin(
            @PathVariable UUID checkinId,
            @AuthenticationPrincipal SecurityUserPrincipal principal
    ) {
        deleteCheckinUseCase.execute(new DeleteCheckinCommand(checkinId, principal.getId()));
    }
}
