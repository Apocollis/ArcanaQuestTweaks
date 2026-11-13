package com.apocollis.aqtweaks.atum;

/**
 * Pure ramp for the Atum sandstorm warning (no Minecraft types, unit tested). Input is the ticks until the
 * provider flips to a storm.
 */
public final class AtumWarningRamp {
    /** Atum pins {@code stormTime} at 1 or 2 while vanilla clean-weather time is above zero; that is not a storm. */
    public static final int PINNED_CLEAN_WEATHER = 2;

    private AtumWarningRamp() {}

    /**
     * 0 while storming, when the countdown is not inside the warning window, or when it is the pinned
     * clean-weather value; otherwise 0..1, rising (eased) as the storm gets closer.
     */
    public static float level(boolean storming, int stormTimeTicks, int warningTicks) {
        if (storming || warningTicks <= 0) return 0.0F;
        if (stormTimeTicks <= PINNED_CLEAN_WEATHER || stormTimeTicks > warningTicks) return 0.0F;
        float x = 1.0F - (float) stormTimeTicks / (float) warningTicks;
        float c = Math.max(0.0F, Math.min(1.0F, x));
        return c * c * (3.0F - 2.0F * c);
    }
}
