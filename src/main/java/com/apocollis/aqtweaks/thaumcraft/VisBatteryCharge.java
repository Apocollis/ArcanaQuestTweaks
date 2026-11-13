package com.apocollis.aqtweaks.thaumcraft;

/** Pure charge/damage mapping for the portable Vis Battery (no Minecraft types). */
public final class VisBatteryCharge {
    public static final int MAX = 10;

    private VisBatteryCharge() {}

    /** Item damage that carries {@code charge}; clamped because the block state throws outside 0..10. */
    public static int damageFor(int charge) {
        return Math.max(0, Math.min(MAX, charge));
    }
}
