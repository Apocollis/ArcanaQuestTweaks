package com.apocollis.aqtweaks.aether;

/** Gear rules of the Silver Squalls as pure functions of what the player wears. No Minecraft types. */
public final class AetherSquallGear {
    private AetherSquallGear() {}

    /** Wind push multiplier: a full insulated set is the most stable, conductive metal gear is heavier. */
    public static double pushFactor(boolean insulatedSet, boolean conductive) {
        if (insulatedSet) return 0.60D;
        if (conductive) return 0.75D;
        return 1.0D;
    }

    /** Relative chance to be picked for a lightning strike; insulated sets avoid lock-on. */
    public static int attractionWeight(boolean insulatedSet, boolean conductive) {
        if (insulatedSet) return 1;
        return conductive ? 4 : 1;
    }

    /** Only a conductive, non-insulated player is hit directly; others get a bolt on nearby high ground. */
    public static boolean locksOn(boolean insulatedSet, boolean conductive) {
        return conductive && !insulatedSet;
    }

    public static double shockDamage(boolean insulatedSet, double normal, double insulated) {
        return insulatedSet ? insulated : normal;
    }
}
