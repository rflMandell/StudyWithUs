package com.studywithus.analytics.application;

import com.studywithus.analytics.domain.StudyAnalyticsRepository;
import com.studywithus.analytics.domain.StudySubjectAggregate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetSubjectDistributionUseCase {

    public record SubjectDistributionResult(
            String subject,
            long totalMinutes,
            double totalHours,
            int sessionsCount,
            double percentage
    ) {}

    private final StudyAnalyticsRepository repository;

    @Transactional(readOnly = true)
    public List<SubjectDistributionResult> execute(UUID userId) {
        List<StudySubjectAggregate> aggregates = repository.findSubjectAggregatesByUserId(userId).stream()
                .filter(a -> a.getSessionsCount() > 0)
                .toList();

        long grandTotalMinutes = aggregates.stream().mapToLong(StudySubjectAggregate::getTotalMinutes).sum();
        int grandTotalSessions = aggregates.stream().mapToInt(StudySubjectAggregate::getSessionsCount).sum();

        return aggregates.stream()
                .map(agg -> {
                    double pct;
                    if (grandTotalMinutes > 0) {
                        pct = Math.round(((double) agg.getTotalMinutes() / grandTotalMinutes) * 10000.0) / 100.0;
                    } else if (grandTotalSessions > 0) {
                        pct = Math.round(((double) agg.getSessionsCount() / grandTotalSessions) * 10000.0) / 100.0;
                    } else {
                        pct = 0.0;
                    }
                    double hours = Math.round((agg.getTotalMinutes() / 60.0) * 100.0) / 100.0;
                    return new SubjectDistributionResult(
                            agg.getSubject(),
                            agg.getTotalMinutes(),
                            hours,
                            agg.getSessionsCount(),
                            pct
                    );
                })
                .toList();
    }
}
