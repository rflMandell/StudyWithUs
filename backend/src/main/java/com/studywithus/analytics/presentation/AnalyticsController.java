package com.studywithus.analytics.presentation;

import com.studywithus.analytics.application.GetStudyHeatmapUseCase;
import com.studywithus.analytics.application.GetStudySummaryUseCase;
import com.studywithus.analytics.application.GetSubjectDistributionUseCase;
import com.studywithus.identity.infrastructure.SecurityUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final GetStudySummaryUseCase getStudySummaryUseCase;
    private final GetStudyHeatmapUseCase getStudyHeatmapUseCase;
    private final GetSubjectDistributionUseCase getSubjectDistributionUseCase;

    @GetMapping("/study-summary")
    public ResponseEntity<GetStudySummaryUseCase.StudySummaryResult> getStudySummary(
            @RequestParam(defaultValue = "30") int days,
            @AuthenticationPrincipal SecurityUserPrincipal principal
    ) {
        return ResponseEntity.ok(getStudySummaryUseCase.execute(principal.getId(), days));
    }

    @GetMapping("/heatmap")
    public ResponseEntity<List<GetStudyHeatmapUseCase.HeatmapDayResult>> getHeatmap(
            @RequestParam(required = false) Integer year,
            @AuthenticationPrincipal SecurityUserPrincipal principal
    ) {
        return ResponseEntity.ok(getStudyHeatmapUseCase.execute(principal.getId(), year));
    }

    @GetMapping("/subjects")
    public ResponseEntity<List<GetSubjectDistributionUseCase.SubjectDistributionResult>> getSubjects(
            @AuthenticationPrincipal SecurityUserPrincipal principal
    ) {
        return ResponseEntity.ok(getSubjectDistributionUseCase.execute(principal.getId()));
    }
}
