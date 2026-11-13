package com.apocollis.aqtweaks.nether;

import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

/**
 * Incremental flood fill of the open cells an ash cloud occupies. Six-neighbour breadth-first search from a seed
 * inside an ellipsoid reach, stopping at blocked cells, run in slices so one tick never does the whole job.
 * Positions are packed like {@code BlockPos.toLong}. No Minecraft types.
 */
public final class AshCloudField {

    public interface Open {
        boolean isOpen(int x, int y, int z);
    }

    private static final int NUM_X_BITS = 26;
    private static final int NUM_Z_BITS = 26;
    private static final int Y_SHIFT = NUM_Z_BITS;
    private static final int X_SHIFT = Y_SHIFT + 12;
    private static final long X_MASK = (1L << NUM_X_BITS) - 1L;
    private static final long Y_MASK = (1L << 12) - 1L;
    private static final long Z_MASK = (1L << NUM_Z_BITS) - 1L;

    public static long pack(int x, int y, int z) {
        return ((long) x & X_MASK) << X_SHIFT | ((long) y & Y_MASK) << Y_SHIFT | (long) z & Z_MASK;
    }

    public static int unpackX(long p) {
        return (int) (p << 64 - X_SHIFT - NUM_X_BITS >> 64 - NUM_X_BITS);
    }

    public static int unpackY(long p) {
        return (int) (p << 64 - Y_SHIFT - 12 >> 64 - 12);
    }

    public static int unpackZ(long p) {
        return (int) (p << 64 - NUM_Z_BITS >> 64 - NUM_Z_BITS);
    }

    private final Open open;
    private final double cx;
    private final double cy;
    private final double cz;
    private final double rh;
    private final double rv;
    private final int maxCells;
    private final LongOpenHashSet seen = new LongOpenHashSet();
    private final LongArrayFIFOQueue queue = new LongArrayFIFOQueue();
    private boolean done;

    /** @param previous the occupied set of the last fill (may be null), used to pick a seed that cannot jump a wall */
    public AshCloudField(Open open, double cx, double cy, double cz, double rh, double rv, LongOpenHashSet previous,
                         int maxCells) {
        this.open = open;
        this.cx = cx;
        this.cy = cy;
        this.cz = cz;
        this.rh = Math.max(1.0D, rh);
        this.rv = Math.max(1.0D, rv);
        this.maxCells = Math.max(1, maxCells);
        long seed = chooseSeed(previous);
        if (seed == Long.MIN_VALUE) {
            done = true;
        } else {
            seen.add(seed);
            queue.enqueue(seed);
        }
    }

    private long chooseSeed(LongOpenHashSet previous) {
        int sx = (int) Math.floor(cx);
        int sy = (int) Math.floor(cy);
        int sz = (int) Math.floor(cz);
        if (previous == null || previous.isEmpty()) {
            for (int dy = 0; dy <= 3; dy++) {
                for (int s = 0; s <= 2; s++) {
                    for (int dx = -s; dx <= s; dx++) {
                        for (int dz = -s; dz <= s; dz++) {
                            for (int sign = -1; sign <= 1; sign += 2) {
                                int y = sy + sign * dy;
                                if (inside(sx + dx, y, sz + dz) && open.isOpen(sx + dx, y, sz + dz)) {
                                    return pack(sx + dx, y, sz + dz);
                                }
                            }
                        }
                    }
                }
            }
            return Long.MIN_VALUE;
        }
        long centre = pack(sx, sy, sz);
        if (previous.contains(centre) && inside(sx, sy, sz) && open.isOpen(sx, sy, sz)) {
            return centre;
        }
        long best = Long.MIN_VALUE;
        double bestD = Double.MAX_VALUE;
        LongIterator it = previous.iterator();
        while (it.hasNext()) {
            long p = it.nextLong();
            int x = unpackX(p);
            int y = unpackY(p);
            int z = unpackZ(p);
            if (!inside(x, y, z) || !open.isOpen(x, y, z)) continue;
            double d = (x - cx) * (x - cx) + (y - cy) * (y - cy) + (z - cz) * (z - cz);
            if (d < bestD) {
                bestD = d;
                best = p;
            }
        }
        return best;
    }

    private boolean inside(int x, int y, int z) {
        double dx = (x + 0.5D - cx) / rh;
        double dy = (y + 0.5D - cy) / rv;
        double dz = (z + 0.5D - cz) / rh;
        return dx * dx + dy * dy + dz * dz <= 1.0D;
    }

    /** Runs up to {@code budget} node expansions. True when the fill is finished (or capped). */
    public boolean step(int budget) {
        if (done) return true;
        int n = 0;
        while (!queue.isEmpty() && n < budget && seen.size() < maxCells) {
            long p = queue.dequeueLong();
            n++;
            int x = unpackX(p);
            int y = unpackY(p);
            int z = unpackZ(p);
            visit(x + 1, y, z);
            visit(x - 1, y, z);
            visit(x, y + 1, z);
            visit(x, y - 1, z);
            visit(x, y, z + 1);
            visit(x, y, z - 1);
        }
        if (queue.isEmpty() || seen.size() >= maxCells) {
            done = true;
        }
        return done;
    }

    private void visit(int x, int y, int z) {
        if (!inside(x, y, z)) return;
        long p = pack(x, y, z);
        if (seen.contains(p)) return;
        if (!open.isOpen(x, y, z)) return;
        seen.add(p);
        queue.enqueue(p);
    }

    public boolean isDone() {
        return done;
    }

    public LongOpenHashSet result() {
        return seen;
    }
}
