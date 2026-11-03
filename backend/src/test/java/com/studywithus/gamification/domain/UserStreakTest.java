package com.studywithus.gamification.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserStreakTest {

    @Test
    @DisplayName("Should increment streak on consecutive days: Day 1 -> 1, Day 2 -> 2, Day 3 -> 3")
    void shouldIncrementStreakOnConsecutiveDays() {
        UserStreak streak = UserStreak.empty(UUID.randomUUID());

        LocalDate day1 = LocalDate.of(2026, 11, 1);
        streak.recordActivity(day1, List.of());
        assertThat(streak.getCurrentStreak()).isEqualTo(1);
        assertThat(streak.getLongestStreak()).isEqualTo(1);
        assertThat(streak.getLastActivityDate()).isEqualTo(day1);

        LocalDate day2 = LocalDate.of(2026, 11, 2);
        streak.recordActivity(day2, List.of());
        assertThat(streak.getCurrentStreak()).isEqualTo(2);
        assertThat(streak.getLongestStreak()).isEqualTo(2);

        LocalDate day3 = LocalDate.of(2026, 11, 3);
        streak.recordActivity(day3, List.of());
        assertThat(streak.getCurrentStreak()).isEqualTo(3);
        assertThat(streak.getLongestStreak()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should maintain streak when activity is repeated on the same day")
    void shouldNotChangeStreakOnSameDay() {
        UserStreak streak = UserStreak.empty(UUID.randomUUID());
        LocalDate day1 = LocalDate.of(2026, 11, 1);

        streak.recordActivity(day1, List.of());
        assertThat(streak.getCurrentStreak()).isEqualTo(1);

        // Activity on same day
        streak.recordActivity(day1, List.of());
        assertThat(streak.getCurrentStreak()).isEqualTo(1);
        assertThat(streak.getLongestStreak()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should break streak when there is a gap with non-free days: Day 3 -> Day 5 (break to 1)")
    void shouldBreakStreakOnMissedDay() {
        UserStreak streak = UserStreak.empty(UUID.randomUUID());

        streak.recordActivity(LocalDate.of(2026, 11, 1), List.of());
        streak.recordActivity(LocalDate.of(2026, 11, 2), List.of());
        streak.recordActivity(LocalDate.of(2026, 11, 3), List.of());
        assertThat(streak.getCurrentStreak()).isEqualTo(3);
        assertThat(streak.getLongestStreak()).isEqualTo(3);

        // Day 4 missed, activity on Day 5 -> resets to 1, longest remains 3
        LocalDate day5 = LocalDate.of(2026, 11, 5);
        streak.recordActivity(day5, List.of());
        assertThat(streak.getCurrentStreak()).isEqualTo(1);
        assertThat(streak.getLongestStreak()).isEqualTo(3);
        assertThat(streak.getLastActivityDate()).isEqualTo(day5);
    }

    @Test
    @DisplayName("Should preserve streak across free days: Day 3 (Fri) -> Sat & Sun are free days -> Day 6 (Mon) continues to 4")
    void shouldPreserveStreakAcrossFreeDays() {
        UserStreak streak = UserStreak.empty(UUID.randomUUID());

        // 2026-11-06 is Friday (day 5 of week)
        LocalDate friday = LocalDate.of(2026, 11, 6);
        streak.recordActivity(friday.minusDays(2), List.of(6, 7)); // Wed -> 1
        streak.recordActivity(friday.minusDays(1), List.of(6, 7)); // Thu -> 2
        streak.recordActivity(friday, List.of(6, 7));              // Fri -> 3

        assertThat(streak.getCurrentStreak()).isEqualTo(3);

        // Saturday (7) and Sunday (8) are free days (days 6 and 7 of week)
        // Activity on Monday 2026-11-09
        LocalDate monday = LocalDate.of(2026, 11, 9);
        streak.recordActivity(monday, List.of(6, 7));

        assertThat(streak.getCurrentStreak()).isEqualTo(4);
        assertThat(streak.getLongestStreak()).isEqualTo(4);
        assertThat(streak.getLastActivityDate()).isEqualTo(monday);
    }
}
