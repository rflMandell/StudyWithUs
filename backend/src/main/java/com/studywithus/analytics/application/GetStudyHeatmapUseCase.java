package com.studywithus.analytics.application;

import com.studywithus.analytics.domain.StudyAnalyticsRepository;
import com.studywithus.analytics.domain.StudyDailyAggregate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetStudyHeatmapUseCase {

    public record HeatmapDayResult(
            LocalDate date,
            long minutes,
            int sessionsCount,
            int intensityLevel
    ) {}

    private final StudyAnalyticsRepository repository;

    @Transactional(readOnly = true)
    public List<HeatmapDayResult> execute(UUID userId, Integer year) {
        LocalDate end = year != null
                ? LocalDate.of(year, 12, 31)
                : LocalDate.now(ZoneOffset.UTC);
        LocalDate start = year != null
                ? LocalDate.of(year, 1, 1)
                : end.minusDays(364);

        return repository.findDailyAggregatesBetween(userId, start, end).stream()
                .filter(agg -> agg.getSessionsCount() > 0)
                .map(this::toHeatmapDay)
                .toList();
    }

    private HeatmapDayResult toHeatmapDay(StudyDailyAggregate agg) {
        return new HeatmapDayResult(
                agg.getActivityDate(),
                agg.getTotalMinutes(),
                agg.getSessionsCount(),
                computeIntensity(agg.getTotalMinutes(), agg.getSessionsCount())
        );
    }

    public static int computeIntensity(long minutes, int sessionsCount) {
        if (sessionsCount <= 0) {
            return 0;
        }
        if (minutes >= 120) {
            return 4;
        }
        if (minutes >= 60) {
            return 3;
        }
        if (minutes >= 30) {
            return 2;
        }
        return 1;
    }
}
