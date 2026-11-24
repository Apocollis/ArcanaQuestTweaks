package com.apocollis.aqtweaks.stamina;

/** Pure interval rules for the state-based stamina refill (plan 20261122-0610). Ticks per half-feather. */
public final class StaminaRegenMath {

    public enum Pace { RESTING, MOVING, ACTIVE }

    /** Large value meaning "no Replenishment cap". */
    public static final int NO_CAP = Integer.MAX_VALUE;

    private StaminaRegenMath() {}

    /**
     * Replenishment's Active interval: {@code base - step * amplifier}, never below {@code minimum}. A base of 0 (or
     * less) disables it, and a negative amplifier means the potion is absent.
     */
    public static int replenishment(int amplifier, int base, int step, int minimum) {
        if (amplifier < 0 || base <= 0) return NO_CAP;
        long raw = (long) base - (long) Math.max(0, step) * amplifier;
        return (int) Math.max(Math.max(1, minimum), raw);
    }

    /**
     * Final interval for one pace. {@code cardioMaster} makes Active use the Moving interval. {@code sprintLike} is
     * Marathon Runner's sprint and jump: Moving, but never worse than what Active would give with the other
     * modifiers. Vigor (the Rested interval, 30 when not Rested) caps every pace.
     */
    public static int interval(Pace pace, boolean cardioMaster, boolean sprintLike, int resting, int moving,
            int active, int vigor, int replenishment) {
        int value;
        switch (pace) {
            case RESTING: value = resting; break;
            case MOVING: value = moving; break;
            default: value = cardioMaster ? Math.min(active, moving) : active; break;
        }
        if (pace == Pace.ACTIVE || (sprintLike && pace == Pace.MOVING)) {
            value = Math.min(value, replenishment);
        }
        value = Math.min(value, vigor);
        return Math.max(1, value);
    }

    /** Ticks a melee swing keeps the player Active: the base combo grace plus the weapon's attack period. */
    public static int swingWindow(int comboBaseGrace, int attackPeriodTicks) {
        return Math.max(0, comboBaseGrace) + Math.max(1, attackPeriodTicks);
    }
}
