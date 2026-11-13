package com.apocollis.aqtweaks.nether;

import java.util.Random;

/** Eruption times for one player inside one spout event, as tick offsets from now. No Minecraft types. */
public final class SpoutSchedule {
    private SpoutSchedule() {}

    /**
     * {@code count} random in [minCount, maxCount]; the first at 0 to {@code firstMaxTicks}, each next
     * {@code staggerMin..staggerMax} ticks after the previous. Eruptions that would start at or after
     * {@code windowTicks} are dropped.
     */
    public static long[] draw(Random rand, int minCount, int maxCount, int firstMaxTicks, int staggerMinTicks,
                              int staggerMaxTicks, long windowTicks) {
        int lo = Math.max(0, Math.min(minCount, maxCount));
        int hi = Math.max(lo, maxCount);
        int count = lo + (hi > lo ? rand.nextInt(hi - lo + 1) : 0);
        long[] tmp = new long[count];
        int n = 0;
        long t = rand.nextInt(Math.max(1, firstMaxTicks + 1));
        for (int i = 0; i < count; i++) {
            if (t >= windowTicks) break;
            tmp[n++] = t;
            int span = Math.max(0, staggerMaxTicks - staggerMinTicks);
            t += staggerMinTicks + (span > 0 ? rand.nextInt(span + 1) : 0);
        }
        long[] out = new long[n];
        System.arraycopy(tmp, 0, out, 0, n);
        return out;
    }
}
