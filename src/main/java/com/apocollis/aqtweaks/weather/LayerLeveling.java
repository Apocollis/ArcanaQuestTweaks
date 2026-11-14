package com.apocollis.aqtweaks.weather;

import java.util.function.LongToIntFunction;

/**
 * Smooth snow: a layer meant for a column goes downhill to the shallowest neighbour that is at least {@code step}
 * layers shallower, so orthogonal neighbours never end up more than {@code step} apart. Columns are packed
 * (x, z) longs. No Minecraft types.
 */
public final class LayerLeveling {
    private LayerLeveling() {}

    public static final long NONE = Long.MIN_VALUE;
    public static final int MAX_HOPS = 3;

    public static long pack(int x, int z) {
        return ((long) x & 0xFFFFFFFFL) | ((long) z << 32);
    }

    public static int unpackX(long p) {
        return (int) p;
    }

    public static int unpackZ(long p) {
        return (int) (p >> 32);
    }

    /**
     * @param depthOf depth in layers of a column, or -1 when the column cannot hold snow (unloaded, roofed, water...);
     *                such neighbours do not constrain
     * @return the column that takes the layer, or {@link #NONE} when the start cannot take one or every candidate is at the cap
     */
    public static long walk(long start, LongToIntFunction depthOf, int step, int cap, int maxHops) {
        int limit = Math.min(Math.max(cap, 1), LayerRate.MAX_LAYERS);
        int st = Math.max(1, step);
        long cur = start;
        int d = depthOf.applyAsInt(cur);
        if (d < 0) return NONE;
        for (int hop = 0; hop <= maxHops; hop++) {
            int x = unpackX(cur);
            int z = unpackZ(cur);
            long best = NONE;
            int bestD = Integer.MAX_VALUE;
            long[] nb = {pack(x + 1, z), pack(x - 1, z), pack(x, z + 1), pack(x, z - 1)};
            for (long n : nb) {
                int nd = depthOf.applyAsInt(n);
                if (nd >= 0 && nd <= d - st && nd < bestD) {
                    best = n;
                    bestD = nd;
                }
            }
            if (best == NONE || hop == maxHops) {
                return d < limit ? cur : NONE;
            }
            cur = best;
            d = bestD;
        }
        return d < limit ? cur : NONE;
    }
}
