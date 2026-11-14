package com.apocollis.aqtweaks.aether;

import java.util.Arrays;
import java.util.Random;

/**
 * Timing of the strike salvos of one lightning storm, as absolute world ticks. No Minecraft types.
 *
 * <p>The first salvo strikes at the end of the storm warning (or, when the storm is entered already active, one
 * telegraph after {@code now}); the rest are spread evenly with a little jitter up to {@code end - 60} ticks, so the
 * 1 to 3 second secondary bolts of the last salvo still land inside the storm.
 */
public final class AetherLightningSchedule {
    private AetherLightningSchedule() {}

    /** Ticks reserved after the last primary strike for its secondary and retarget bolts. */
    public static final int TAIL_TICKS = 60;
    /** Smallest gap between two salvos. */
    public static final int MIN_GAP = 10;

    /**
     * @param now             current world tick
     * @param start           tick the storm becomes active (end of the warning)
     * @param end             tick the storm ends
     * @param telegraphTicks  length of one bolt's glow
     * @param count           number of salvos, at least 1
     * @return strike tick of each salvo's primary bolts, ascending
     */
    public static long[] planWaves(long now, long start, long end, int telegraphTicks, int count, Random rand) {
        int n = Math.max(1, count);
        long t0 = now < start ? start : now + telegraphTicks;
        long last = Math.max(t0 + 40L, end - TAIL_TICKS);
        long[] out = new long[n];
        out[0] = t0;
        if (n == 1) return out;
        double spacing = (double) (last - t0) / (n - 1);
        for (int i = 1; i < n; i++) {
            double jitter = i == n - 1 ? 0.0D : (rand.nextDouble() * 2.0D - 1.0D) * 0.15D * spacing;
            long t = t0 + Math.round(spacing * i + jitter);
            t = Math.max(out[i - 1] + MIN_GAP, Math.min(last, t));
            out[i] = t;
        }
        Arrays.sort(out);
        return out;
    }
}
