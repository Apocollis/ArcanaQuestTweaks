package com.apocollis.aqtweaks.twilightforest;

/** Pure numbers of the Ley-Rupture: scour damage, charging and the warp caps. No Minecraft types. */
public final class SurgeMath {
    private SurgeMath() {}

    /** Scour damage for one hit: tier damage scaled by exposure and biome intensity. */
    public static double scour(boolean properGear, double unprotected, double proper, double exposure,
                               double intensity) {
        return (properGear ? proper : unprotected) * exposure * intensity;
    }

    /** Charge gained by one pulse, in the item's own units. */
    public static double chargeAmount(double capacity, double percent, double exposure, double intensity) {
        return capacity * (percent / 100.0D) * exposure * intensity;
    }

    /** How much of {@code wanted} warp can still be given without exceeding {@code cap} for this surge. */
    public static int warpAllowed(int already, int wanted, int cap) {
        return Math.max(0, Math.min(wanted, cap - already));
    }

    /** Backlash scales with exposure and takes the proper-gear tier ratio. */
    public static double backlash(double base, double exposure, boolean properGear, double unprotected, double proper) {
        double tier = properGear && unprotected > 0.0D ? proper / unprotected : 1.0D;
        return base * exposure * tier;
    }
}
