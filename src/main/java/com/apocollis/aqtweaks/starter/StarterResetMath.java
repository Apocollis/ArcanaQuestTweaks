package com.apocollis.aqtweaks.starter;

import java.util.function.IntUnaryOperator;

/** Pure refund math of the Tabula Rasa draught (kept apart so it can be tested). */
public final class StarterResetMath {

    private StarterResetMath() {}

    /** The level the previous class gave for free: its starter target, held to the skill cap, never below 1. */
    public static int baseLevel(int starterTarget, int cap) {
        return Math.max(1, Math.min(starterTarget, cap));
    }

    public static int purchasedLevels(int currentLevel, int baseLevel) {
        return Math.max(0, currentLevel - baseLevel);
    }

    /**
     * Vanilla XP points spent on the levels the player bought: each step from {@code lvl} to {@code lvl + 1} cost
     * {@code xpForLevel(levelUpCostAt(lvl))}. Free starter levels refund nothing. Clamped to the int range.
     */
    public static int refundXpForSkill(int currentLevel, int baseLevel,
            IntUnaryOperator levelUpCostAt, IntUnaryOperator xpForLevel) {
        long total = 0L;
        for (int lvl = Math.max(1, baseLevel); lvl < currentLevel; lvl++) {
            total += Math.max(0, xpForLevel.applyAsInt(levelUpCostAt.applyAsInt(lvl)));
        }
        return (int) Math.min(total, Integer.MAX_VALUE);
    }

    /** Adds refunds without overflowing an int. */
    public static int addClamped(int a, int b) {
        return (int) Math.min((long) a + (long) b, Integer.MAX_VALUE);
    }
}
