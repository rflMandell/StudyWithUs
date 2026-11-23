package com.studywithus.gamification.domain;

public final class LevelCurve {

    public static final long BASE_XP_PER_LEVEL = 100L;

    public record LevelProgress(
            int level,
            long totalXp,
            long currentLevelXp,
            long nextLevelXp,
            double progressPercent
    ) {}

    private LevelCurve() {}

    public static long xpRequiredToAdvanceFrom(int level) {
        int safeLevel = Math.max(level, 1);
        return BASE_XP_PER_LEVEL * safeLevel;
    }

    public static LevelProgress calculate(long totalXp) {
        long safeTotalXp = Math.max(totalXp, 0L);
        int level = 1;
        long remaining = safeTotalXp;

        while (true) {
            long needed = xpRequiredToAdvanceFrom(level);
            if (remaining < needed) {
                double percent = needed > 0
                        ? Math.round(((double) remaining / needed) * 1000.0) / 10.0
                        : 0.0;
                return new LevelProgress(level, safeTotalXp, remaining, needed, percent);
            }
            remaining -= needed;
            level++;
        }
    }
}
