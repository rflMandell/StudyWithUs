package com.studywithus.gamification.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LevelCurveTest {

    @Test
    void shouldStartAtLevelOneWithZeroXp() {
        LevelCurve.LevelProgress progress = LevelCurve.calculate(0);
        assertThat(progress.level()).isEqualTo(1);
        assertThat(progress.currentLevelXp()).isEqualTo(0);
        assertThat(progress.nextLevelXp()).isEqualTo(100);
        assertThat(progress.progressPercent()).isEqualTo(0.0);
    }

    @Test
    void shouldCalculateExactLevelTransitionsAndMultiLevelJumps() {
        // 100 XP -> Level 2 (0 / 200)
        LevelCurve.LevelProgress l2 = LevelCurve.calculate(100);
        assertThat(l2.level()).isEqualTo(2);
        assertThat(l2.currentLevelXp()).isEqualTo(0);
        assertThat(l2.nextLevelXp()).isEqualTo(200);

        // 150 XP -> Level 2 (50 / 200 = 25%)
        LevelCurve.LevelProgress l2Mid = LevelCurve.calculate(150);
        assertThat(l2Mid.level()).isEqualTo(2);
        assertThat(l2Mid.currentLevelXp()).isEqualTo(50);
        assertThat(l2Mid.progressPercent()).isEqualTo(25.0);

        // 100 + 200 + 300 + 400 = 1000 XP -> Level 5
        LevelCurve.LevelProgress l5 = LevelCurve.calculate(1000);
        assertThat(l5.level()).isEqualTo(5);
        assertThat(l5.currentLevelXp()).isEqualTo(0);
        assertThat(l5.nextLevelXp()).isEqualTo(500);
    }
}
