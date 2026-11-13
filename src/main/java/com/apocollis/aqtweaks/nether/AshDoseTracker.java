package com.apocollis.aqtweaks.nether;

/** Ash dose bookkeeping: builds while inside a cloud, decays at twice the gain outside. No Minecraft types. */
public final class AshDoseTracker {
    private AshDoseTracker() {}

    /** One second of dose change. {@code gain} is the per-second gain for the player's gear tier. */
    public static double update(double dose, boolean inside, double gain, double cap) {
        double next = inside ? dose + gain : dose - 2.0D * Math.max(0.1D, gain);
        return Math.max(0.0D, Math.min(cap, next));
    }

    /** 0 none, 1 Slowness I and Wither I, 2 Wither II and blindness pulses, 3 Wither III. */
    public static int level(double dose, double t1, double t2, double t3) {
        if (dose >= t3) return 3;
        if (dose >= t2) return 2;
        if (dose >= t1) return 1;
        return 0;
    }
}
