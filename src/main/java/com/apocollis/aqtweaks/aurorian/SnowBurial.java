package com.apocollis.aqtweaks.aurorian;

/** Pure burial rules for the storm snow hazard. No Minecraft types. */
public final class SnowBurial {
    private SnowBurial() {}

    public enum Level {
        NONE, BODY, HEAD
    }

    public static final int AIR_MAX = 300;

    /** Head when the eye is under the snow top, body when only the chest is, otherwise none. */
    public static Level level(double chestY, double eyeY, double snowTopY) {
        if (eyeY < snowTopY) return Level.HEAD;
        if (chestY < snowTopY) return Level.BODY;
        return Level.NONE;
    }

    /** New air debt after one second with the head under: the drain is added, capped just above empty. */
    public static int debtWithHeadUnder(int debt, int drain) {
        if (drain <= 0) return Math.max(0, debt);
        return Math.min(AIR_MAX + 20, Math.max(0, debt) + drain);
    }

    /** New air debt after one second with the head free: it refills quickly. */
    public static int debtWithHeadFree(int debt) {
        return Math.max(0, debt - 160);
    }

    /** True once the debt has used up the air. */
    public static boolean outOfAir(int debt) {
        return debt >= AIR_MAX;
    }

    /** Suffocation damage that leaves the player at 1 heart at the least: min(damage, health - 1), never below 0. */
    public static float damage(float health, float configured) {
        if (configured <= 0.0F) return 0.0F;
        return Math.max(0.0F, Math.min(configured, health - 1.0F));
    }
}
