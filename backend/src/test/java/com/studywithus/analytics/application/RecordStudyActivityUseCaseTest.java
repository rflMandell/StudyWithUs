package com.studywithus.analytics.application;

import com.studywithus.analytics.domain.StudyActivityRecord;
import com.studywithus.analytics.domain.StudyAnalyticsRepository;
import com.studywithus.analytics.domain.StudyDailyAggregate;
import com.studywithus.analytics.domain.StudySubjectAggregate;
import com.studywithus.checkin.domain.StudyActivityRecordedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecordStudyActivityUseCaseTest {

    @Mock
    private StudyAnalyticsRepository repository;

    private RecordStudyActivityUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new RecordStudyActivityUseCase(repository);
    }

    @Test
    void shouldRecordDailyAndSubjectAggregatesAndBeIdempotent() {
        UUID checkinId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant now = Instant.parse("2026-11-23T15:00:00Z");

        when(repository.existsRecordByCheckinId(checkinId)).thenReturn(false);
        when(repository.findDailyAggregate(userId, LocalDate.of(2026, 11, 23))).thenReturn(Optional.empty());
        when(repository.findSubjectAggregate(userId, "Cálculo")).thenReturn(Optional.empty());

        useCase.record(new StudyActivityRecordedEvent(
                checkinId, userId, UUID.randomUUID(), null, List.of("Cálculo", "Derivadas"), 60, now
        ));

        ArgumentCaptor<StudyDailyAggregate> dailyCaptor = ArgumentCaptor.forClass(StudyDailyAggregate.class);
        verify(repository).saveDailyAggregate(dailyCaptor.capture());
        assertThat(dailyCaptor.getValue().getTotalMinutes()).isEqualTo(60L);
        assertThat(dailyCaptor.getValue().getSessionsCount()).isEqualTo(1);

        ArgumentCaptor<StudySubjectAggregate> subjectCaptor = ArgumentCaptor.forClass(StudySubjectAggregate.class);
        verify(repository).saveSubjectAggregate(subjectCaptor.capture());
        assertThat(subjectCaptor.getValue().getSubject()).isEqualTo("Cálculo");
        assertThat(subjectCaptor.getValue().getTotalMinutes()).isEqualTo(60L);

        // Idempotent duplicate call
        when(repository.existsRecordByCheckinId(checkinId)).thenReturn(true);
        useCase.record(new StudyActivityRecordedEvent(
                checkinId, userId, UUID.randomUUID(), "Cálculo", List.of(), 60, now
        ));
        verify(repository, org.mockito.Mockito.times(1)).saveRecord(any(StudyActivityRecord.class));
    }

    @Test
    void shouldReverseAggregatesOnCheckinDeletion() {
        UUID checkinId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 11, 23);

        StudyActivityRecord record = StudyActivityRecord.create(checkinId, userId, date, "Física", 45);
        StudyDailyAggregate daily = StudyDailyAggregate.create(userId, date);
        daily.addSession(45);
        StudySubjectAggregate subject = StudySubjectAggregate.create(userId, "Física");
        subject.addSession(45);

        when(repository.findRecordByCheckinId(checkinId)).thenReturn(Optional.of(record));
        when(repository.findDailyAggregate(userId, date)).thenReturn(Optional.of(daily));
        when(repository.findSubjectAggregate(userId, "Física")).thenReturn(Optional.of(subject));

        useCase.reverse(checkinId);

        assertThat(daily.getTotalMinutes()).isEqualTo(0L);
        assertThat(daily.getSessionsCount()).isEqualTo(0);
        assertThat(subject.getTotalMinutes()).isEqualTo(0L);
        verify(repository).deleteRecordByCheckinId(checkinId);
    }
}
