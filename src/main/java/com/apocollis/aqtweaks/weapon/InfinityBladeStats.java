package com.apocollis.aqtweaks.weapon;

import java.util.Locale;

/** Pure maths for the Infinity Blade stats, so it is unit tested without the Minecraft bootstrap. */
public final class InfinityBladeStats {
    private InfinityBladeStats() {}

    /** A player's base attack damage, which the weapon modifier adds to. */
    public static final double PLAYER_BASE_DAMAGE = 1.0D;
    /** A player's base attack speed (attacks per second). */
    public static final double PLAYER_BASE_SPEED = 4.0D;

    /** The attack-damage modifier amount that makes the total equal the configured damage. Never negative. */
    public static double damageModifier(double configuredDamage) {
        return Math.max(0.0D, configuredDamage - PLAYER_BASE_DAMAGE);
    }

    /** The attack-speed modifier amount that makes the total equal the configured attacks per second. */
    public static double speedModifier(double configuredSpeed) {
        return configuredSpeed - PLAYER_BASE_SPEED;
    }

    /** True when the repair item's registry name equals the configured one. {@code none} or empty disables repair. */
    public static boolean repairMatches(String configuredName, String registryName) {
        if (configuredName == null || registryName == null) return false;
        String cfg = configuredName.trim();
        if (cfg.isEmpty() || cfg.toLowerCase(Locale.ROOT).equals("none")) return false;
        return cfg.equals(registryName);
    }
}
