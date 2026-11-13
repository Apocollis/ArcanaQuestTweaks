package com.apocollis.aqtweaks.paragon;

/**
 * Zanite's wear scaling as a fraction of the stack's own durability. Aether's Zanite has 250 uses and steps up at 50, 110,
 * 200 and 239 of them left; the same thresholds as fractions keep the feel on a much longer-lived Tartarite item.
 */
public final class WearTier {
    private WearTier() {}

    public static final int TIERS = 5;
    /** Share of durability left at or below which each tier from 1 to 4 applies: 96, 80, 44 and 20 percent. */
    private static final double[] LEFT_AT_OR_BELOW = {239.0D / 250.0D, 200.0D / 250.0D, 110.0D / 250.0D, 50.0D / 250.0D};
    /** Aether's tool speeds by tier. */
    private static final float[] AETHER_SPEED = {2.0F, 4.0F, 6.0F, 8.0F, 12.0F};

    /** 0 for an unworn stack up to 4 for one nearly broken. */
    public static int tier(int damage, int maxDamage) {
        if (maxDamage <= 0) return 0;
        double left = 1.0D - (double) Math.max(0, damage) / maxDamage;
        int tier = 0;
        for (int i = 0; i < LEFT_AT_OR_BELOW.length; i++) {
            if (left <= LEFT_AT_OR_BELOW[i] + 1e-9D) tier = i + 1;
        }
        return tier;
    }

    /** Mining speed factor: Aether's tool speed over its iron-class speed of 4, never below 1 so a fresh item is not slowed. */
    public static float speedFactor(int tier) {
        return Math.max(1.0F, AETHER_SPEED[Math.max(0, Math.min(TIERS - 1, tier))] / 4.0F);
    }

    /** Extra weapon damage by tier: Aether's sword runs 3 to 7, so a bonus of 0 to 4 over the base. */
    public static float swordBonus(int tier) {
        return Math.max(0, Math.min(TIERS - 1, tier));
    }
}
