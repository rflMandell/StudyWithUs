package com.studywithus.moderation.presentation;

import com.studywithus.identity.infrastructure.SecurityUserPrincipal;
import com.studywithus.moderation.application.CreateReportCommand;
import com.studywithus.moderation.application.CreateReportUseCase;
import com.studywithus.moderation.application.DismissReportCommand;
import com.studywithus.moderation.application.DismissReportUseCase;
import com.studywithus.moderation.application.GetReportUseCase;
import com.studywithus.moderation.application.ListReportsQuery;
import com.studywithus.moderation.application.ListReportsUseCase;
import com.studywithus.moderation.application.ResolveReportCommand;
import com.studywithus.moderation.application.ResolveReportUseCase;
import com.studywithus.moderation.domain.ReportStatus;
import com.studywithus.moderation.domain.ReportTargetType;
import com.studywithus.shared.presentation.PagedResponse;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final CreateReportUseCase createReportUseCase;
    private final ListReportsUseCase listReportsUseCase;
    private final GetReportUseCase getReportUseCase;
    private final ResolveReportUseCase resolveReportUseCase;
    private final DismissReportUseCase dismissReportUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<ReportResponse> createReport(
            @AuthenticationPrincipal SecurityUserPrincipal principal,
            @Valid @RequestBody CreateReportRequest request
    ) {
        var command = new CreateReportCommand(
                principal.getId(),
                request.targetType(),
                request.targetId(),
                request.reason(),
                request.description()
        );
        var result = createReportUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(ReportResponse.from(result));
    }

    @GetMapping
    public ResponseEntity<PagedResponse<ReportResponse>> listReports(
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(required = false) ReportTargetType targetType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        var query = new ListReportsQuery(status, targetType, page, size);
        var pagedResult = listReportsUseCase.execute(query);
        return ResponseEntity.ok(PagedResponse.from(pagedResult, ReportResponse::from));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReportResponse> getReport(@PathVariable UUID id) {
        var result = getReportUseCase.execute(id);
        return ResponseEntity.ok(ReportResponse.from(result));
    }

    @PostMapping("/{id}/resolve")
    public ResponseEntity<ReportResponse> resolveReport(
            @AuthenticationPrincipal SecurityUserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody ResolveReportRequest request
    ) {
        var command = new ResolveReportCommand(
                id,
                principal.getId(),
                request.action(),
                request.notes()
        );
        var result = resolveReportUseCase.execute(command);
        return ResponseEntity.ok(ReportResponse.from(result));
    }

    @PostMapping("/{id}/dismiss")
    public ResponseEntity<ReportResponse> dismissReport(
            @AuthenticationPrincipal SecurityUserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) DismissReportRequest request
    ) {
        String notes = request != null ? request.notes() : null;
        var command = new DismissReportCommand(id, principal.getId(), notes);
        var result = dismissReportUseCase.execute(command);
        return ResponseEntity.ok(ReportResponse.from(result));
    }
}
