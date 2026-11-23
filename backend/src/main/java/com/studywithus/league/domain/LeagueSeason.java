package com.studywithus.league.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeagueSeason {
    private UUID id;
    private String weekKey;
    private Instant startsAt;
    private Instant endsAt;
    private LeagueSeasonStatus status;
    private Instant closedAt;
    private Instant createdAt;

    public static String currentWeekKey(Instant instant) {
        LocalDate date = instant.atZone(ZoneOffset.UTC).toLocalDate();
        int year = date.get(IsoFields.WEEK_BASED_YEAR);
        int week = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        return String.format("%d-W%02d", year, week);
    }

    public static LeagueSeason startNewForWeek(String weekKey, Instant referenceTime) {
        LocalDate monday = referenceTime.atZone(ZoneOffset.UTC)
                .toLocalDate()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        Instant startsAt = monday.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant endsAt = monday.plusDays(7).atStartOfDay(ZoneOffset.UTC).toInstant();
        return LeagueSeason.builder()
                .id(UUID.randomUUID())
                .weekKey(weekKey)
                .startsAt(startsAt)
                .endsAt(endsAt)
                .status(LeagueSeasonStatus.ACTIVE)
                .createdAt(referenceTime)
                .build();
    }

    public void close(Instant closedAt) {
        this.status = LeagueSeasonStatus.CLOSED;
        this.closedAt = closedAt;
    }

    public boolean isActive() {
        return status == LeagueSeasonStatus.ACTIVE;
    }
}
