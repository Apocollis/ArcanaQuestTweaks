package com.apocollis.aqtweaks.thaumadditions;

/** Plain-Java numbers for the Shock focus effect, so the balance rules can be tested without Minecraft. */
public final class ShockMath {
    private ShockMath() {}

    /** Frost's formula: {@code (offset + power) * finalPower}. {@code power} is the effect's own 1 to 5 setting. */
    public static float baseDamage(int offset, int power, float finalPower) {
        return (offset + power) * finalPower;
    }

    /** Damage of jump number {@code jump} (1 = first jump) before any wet bonus: falloff compounds per jump. */
    public static float chainDamage(float primary, float falloff, int jump) {
        float d = primary;
        for (int i = 0; i < jump; i++) {
            d *= falloff;
        }
        return d;
    }

    public static float wetBonus(float damage, boolean wet, float multiplier) {
        return wet ? damage * multiplier : damage;
    }

    /** {@code baseTicks} at duration 1, plus {@code perLevel} for each level above it. */
    public static int stunTicks(int baseTicks, int perLevel, int duration) {
        return Math.max(0, baseTicks + perLevel * (Math.max(1, duration) - 1));
    }

    /** Water conducts: two wet targets link over the wider radius. */
    public static double jumpRadius(boolean currentWet, boolean nextWet, double normal, double wet) {
        return currentWet && nextWet ? Math.max(normal, wet) : normal;
    }

    public static boolean stuns(boolean primary, boolean wet, boolean stunWetChained) {
        return primary || (wet && stunWetChained);
    }
}
