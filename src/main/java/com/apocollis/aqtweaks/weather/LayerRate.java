package com.apocollis.aqtweaks.weather;

/** Pure rate arithmetic for the storm snow accumulation. No Minecraft types. */
public final class LayerRate {
    private LayerRate() {}

    public static final double MIN_STRENGTH = 0.25D;
    public static final int MAX_LAYERS = 16;

    /** Strength curve: nothing below 0.25, then strength squared. */
    public static double curve(double strength) {
        if (strength < MIN_STRENGTH) return 0.0D;
        double s = Math.min(1.0D, strength);
        return s * s;
    }

    /** True while a column at {@code depth} layers may take another, given the cap (never above 16). */
    public static boolean canAdd(int depth, int cap) {
        return depth < Math.min(Math.max(cap, 1), MAX_LAYERS);
    }

    /** Carries the fractional part of the attempts between ticks. */
    public static final class Carry {
        private double carry;

        /** Whole attempts to make this tick for {@code chunks} chunks at {@code ratePerChunkPerSecond}. */
        public int take(int chunks, double ratePerChunkPerSecond, double strength) {
            double want = chunks * ratePerChunkPerSecond * curve(strength) / 20.0D + carry;
            if (want < 0.0D) want = 0.0D;
            int whole = (int) want;
            carry = want - whole;
            return whole;
        }

        public void reset() {
            carry = 0.0D;
        }
    }
}
