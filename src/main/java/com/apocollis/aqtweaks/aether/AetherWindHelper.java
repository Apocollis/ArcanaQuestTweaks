package com.apocollis.aqtweaks.aether;

/**
 * Gust heading for the Silver Squalls. Every client and the server derive the same heading from the gust event
 * seed and the world tick: a new heading every shift interval, blended over a few ticks so changes read as
 * buffeting rather than snaps. No Minecraft types.
 */
public final class AetherWindHelper {
    private AetherWindHelper() {}

    public static final int BLEND_TICKS = 10;

    /** Heading in radians [0, 2 pi) for sub-window {@code window} of the gust with this seed. */
    public static double angleOf(long seed, long window) {
        long z = seed + window * 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        z = z ^ (z >>> 31);
        return ((z >>> 11) * (1.0D / (double) (1L << 53))) * Math.PI * 2.0D;
    }

    /** Heading at {@code tick}: sub-window from {@code start}, blended from the previous heading at its start. */
    public static double headingAt(long seed, long start, long tick, int shiftTicks) {
        int shift = Math.max(1, shiftTicks);
        long elapsed = Math.max(0L, tick - start);
        long window = elapsed / shift;
        double current = angleOf(seed, window);
        long into = elapsed - window * shift;
        if (window > 0 && into < BLEND_TICKS) {
            double previous = angleOf(seed, window - 1);
            double diff = current - previous;
            while (diff > Math.PI) diff -= Math.PI * 2.0D;
            while (diff < -Math.PI) diff += Math.PI * 2.0D;
            double t = (double) into / BLEND_TICKS;
            t = t * t * (3.0D - 2.0D * t);
            return previous + diff * t;
        }
        return current;
    }
}
