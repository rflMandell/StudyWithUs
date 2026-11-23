package com.studywithus.analytics.application;

import com.studywithus.analytics.domain.StudyAnalyticsRepository;
import com.studywithus.analytics.domain.StudyDailyAggregate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetStudySummaryUseCase {

    public record StudySummaryResult(
            int windowDays,
            long totalMinutes,
            double totalHours,
            int totalSessions,
            int activeDays,
            double dailyAverageMinutes,
            double dailyAverageHours,
            double consistencyPercent,
            double hoursToday,
            double hoursThisWeek,
            double hoursThisMonth
    ) {}

    private final StudyAnalyticsRepository repository;

    @Transactional(readOnly = true)
    public StudySummaryResult execute(UUID userId, int days) {
        int windowDays = Math.min(Math.max(days, 1), 365);
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate windowStart = today.minusDays(windowDays - 1L);
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        LocalDate queryStart = windowStart.isBefore(monthStart) ? windowStart : monthStart;
        if (weekStart.isBefore(queryStart)) {
            queryStart = weekStart;
        }

        List<StudyDailyAggregate> aggregates = repository.findDailyAggregatesBetween(userId, queryStart, today);

        long windowMinutes = 0L;
        int windowSessions = 0;
        int activeDays = 0;
        long todayMinutes = 0L;
        long weekMinutes = 0L;
        long monthMinutes = 0L;

        for (StudyDailyAggregate agg : aggregates) {
            LocalDate d = agg.getActivityDate();
            if (!d.isBefore(windowStart)) {
                windowMinutes += agg.getTotalMinutes();
                windowSessions += agg.getSessionsCount();
                if (agg.getSessionsCount() > 0) {
                    activeDays++;
                }
            }
            if (d.equals(today)) {
                todayMinutes += agg.getTotalMinutes();
            }
            if (!d.isBefore(weekStart)) {
                weekMinutes += agg.getTotalMinutes();
            }
            if (!d.isBefore(monthStart)) {
                monthMinutes += agg.getTotalMinutes();
            }
        }

        double totalHours = round2(windowMinutes / 60.0);
        double dailyAvgMinutes = round2((double) windowMinutes / windowDays);
        double dailyAvgHours = round2(dailyAvgMinutes / 60.0);
        double consistencyPercent = round2(Math.min(100.0, ((double) activeDays / windowDays) * 100.0));

        return new StudySummaryResult(
                windowDays,
                windowMinutes,
                totalHours,
                windowSessions,
                activeDays,
                dailyAvgMinutes,
                dailyAvgHours,
                consistencyPercent,
                round2(todayMinutes / 60.0),
                round2(weekMinutes / 60.0),
                round2(monthMinutes / 60.0)
        );
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
