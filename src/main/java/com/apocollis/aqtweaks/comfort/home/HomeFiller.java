package com.apocollis.aqtweaks.comfort.home;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Sliced flood fill that finds the connected interior of a build from the cells beside a bed.
 *
 * <p>Pure: it reads blocks through {@link Probe}. Call {@link #step(int)} each tick with a cell budget until it
 * returns true, then {@link #result()}. If the fill passes {@code maxCells}, reaches an unloaded block, or has
 * no starting cell, the result is the fallback box around the bed instead (open-air or oversized builds).
 *
 * <p><b>Doors.</b> Every door, fence gate and trapdoor ({@link Probe#portal}) is a portal, open or closed. When
 * the fill reaches one it judges the cell on the far side in layers: (1) open sky means outside; (2) 26 rays
 * from the far side, mostly escaping means outside; (3) otherwise a speculative sub-fill from the far side that
 * must close within {@link DoorSettings#probeCells} cells to count as part of the home. An outside door is a
 * wall; nothing past it joins. Door state never changes the answer.
 */
public final class HomeFiller {

    public interface Probe {
        /** True when the block's chunk is loaded. The fill never asks a loaded-state-less world for a block. */
        boolean loaded(int x, int y, int z);

        /** True when a player or mob could occupy the cell (air, plants, ladders and the like). Portals are not. */
        boolean passable(int x, int y, int z);

        /** True when the block is a door, fence gate or trapdoor, whatever its state. */
        boolean portal(int x, int y, int z);

        /**
         * True when the dimension has skylight and nothing that blocks movement or liquid (glass and leaves
         * included) is above the cell. Always false in dimensions without skylight.
         */
        boolean skyExposed(int x, int y, int z);
    }

    /** Tunables for the door test. */
    public static final class DoorSettings {
        public int rayRange = 16;
        public double exteriorFraction = 0.70;
        public double interiorFraction = 0.30;
        public int probeCells = 800;
        public int maxDoors = 24;

        public static DoorSettings defaults() {
            return new DoorSettings();
        }
    }

    private static final int[][] NEIGHBOURS = {
        {0, 1, 0}, {0, -1, 0}, {1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}
    };

    private static final int MAX_DEPTH = 6;

    private static final int TAG_RAYS = 1;
    private static final int TAG_FILL = 2;

    /** One flood: the main fill, or a speculative sub-fill started from the far side of a portal. */
    private static final class Frame {
        final boolean main;
        final ArrayDeque<Long> queue = new ArrayDeque<>();
        final List<Long> cells = new ArrayList<>();
        final Map<Long, Byte> bnd = new HashMap<>();
        final List<Long> decided = new ArrayList<>();
        final long portalKey;
        final int[] portalDir;
        final int cap;
        final int tag;
        boolean failed;
        String failReason;

        Frame(boolean main, long portalKey, int[] portalDir, int cap, int tag) {
            this.main = main;
            this.portalKey = portalKey;
            this.portalDir = portalDir;
            this.cap = cap;
            this.tag = tag;
        }
    }

    private final Probe probe;
    private final int bedX;
    private final int bedY;
    private final int bedZ;
    private final int maxCells;
    private final int fallbackRadius;
    private final int fallbackDown;
    private final int fallbackUp;
    private final DoorSettings doors;

    private final List<Frame> frames = new ArrayList<>();
    private final Set<Long> visited = new HashSet<>();
    /** Cells of discarded sub-fills: space known to lead outside. */
    private final Set<Long> exteriorCells = new HashSet<>();
    /** Door verdicts by portal cell: true interior, false exterior. */
    private final Map<Long, Boolean> decisions = new HashMap<>();
    private final PortalStats stats = new PortalStats();
    private int doorsJudged;
    private boolean seeded;
    private boolean done;
    private String abortReason;
    private HomeRegion result;

    public HomeFiller(Probe probe, int bedX, int bedY, int bedZ, int maxCells,
            int fallbackRadius, int fallbackDown, int fallbackUp) {
        this(probe, bedX, bedY, bedZ, maxCells, fallbackRadius, fallbackDown, fallbackUp, DoorSettings.defaults());
    }

    public HomeFiller(Probe probe, int bedX, int bedY, int bedZ, int maxCells,
            int fallbackRadius, int fallbackDown, int fallbackUp, DoorSettings doors) {
        this.probe = probe;
        this.bedX = bedX;
        this.bedY = bedY;
        this.bedZ = bedZ;
        this.maxCells = Math.max(1, maxCells);
        this.fallbackRadius = Math.max(1, fallbackRadius);
        this.fallbackDown = Math.max(0, fallbackDown);
        this.fallbackUp = Math.max(0, fallbackUp);
        this.doors = doors == null ? DoorSettings.defaults() : doors;
        frames.add(new Frame(true, 0L, null, Integer.MAX_VALUE, 0));
    }

    /** Processes up to {@code budget} cells. @return true when the fill is finished. */
    public boolean step(int budget) {
        if (done) {
            return true;
        }
        if (!seeded) {
            seeded = true;
            seed();
        }
        int processed = 0;
        int limit = Math.max(1, budget);
        while (!done && processed < limit) {
            if (abortReason != null) {
                finish();
                break;
            }
            Frame f = top();
            if (f.failed) {
                discard(f);
                continue;
            }
            if (f.queue.isEmpty()) {
                if (f.main) {
                    finish();
                    break;
                }
                commit(f);
                continue;
            }
            expand(f, f.queue.poll());
            processed++;
        }
        return done;
    }

    public boolean isDone() {
        return done;
    }

    public HomeRegion result() {
        return result;
    }

    public int visitedCount() {
        return visited.size();
    }

    /** Door counters so far (final once the fill is done). */
    public PortalStats portalStats() {
        return stats;
    }

    private Frame top() {
        return frames.get(frames.size() - 1);
    }

    private void seed() {
        // The cell above the bed first, then the ring around and above it.
        int[][] starts = {
            {0, 1, 0}, {1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1},
            {1, 1, 0}, {-1, 1, 0}, {0, 1, 1}, {0, 1, -1}, {0, -1, 0}
        };
        Frame main = frames.get(0);
        for (int[] s : starts) {
            int x = bedX + s[0];
            int y = bedY + s[1];
            int z = bedZ + s[2];
            if (inWorld(y) && probe.loaded(x, y, z) && !probe.portal(x, y, z) && probe.passable(x, y, z)) {
                add(main, x, y, z);
            }
        }
        if (main.queue.isEmpty()) {
            abortReason = "no start cell";
        }
    }

    /** Accepts a cell into a frame. Sets the abort reason (main) or fails the frame (sub-fill) on a cap. */
    private void add(Frame f, int x, int y, int z) {
        long key = HomeRegion.pack(x, y, z);
        if (!visited.add(key)) {
            return;
        }
        f.queue.add(key);
        if (f.main) {
            if (visited.size() > maxCells) {
                abortReason = "cell cap " + maxCells;
            }
            return;
        }
        f.cells.add(key);
        if (f.cells.size() > f.cap) {
            fail(f, "door budget " + f.cap);
        } else if (visited.size() > maxCells) {
            fail(f, "cell cap " + maxCells);
        }
    }

    private void fail(Frame f, String reason) {
        if (!f.failed) {
            f.failed = true;
            f.failReason = reason;
        }
    }

    private boolean stopped(Frame f) {
        return abortReason != null || f.failed;
    }

    private void expand(Frame f, long cell) {
        int cx = HomeRegion.unpackX(cell);
        int cy = HomeRegion.unpackY(cell);
        int cz = HomeRegion.unpackZ(cell);
        for (int[] d : NEIGHBOURS) {
            int x = cx + d[0];
            int y = cy + d[1];
            int z = cz + d[2];
            if (!inWorld(y)) {
                continue;
            }
            long key = HomeRegion.pack(x, y, z);
            if (visited.contains(key)) {
                continue;
            }
            if (!f.main && exteriorCells.contains(key)) {
                fail(f, "joins outside space");
                return;
            }
            if (!probe.loaded(x, y, z)) {
                if (f.main) {
                    abortReason = "unloaded chunk";
                } else {
                    fail(f, "unloaded");
                }
                return;
            }
            if (probe.portal(x, y, z)) {
                int outcome = portal(f, cell, d, x, y, z, key);
                if (outcome == PENDING) {
                    f.queue.addFirst(cell);
                    return;
                }
                if (outcome == STOP) {
                    return;
                }
                continue;
            }
            if (probe.passable(x, y, z)) {
                add(f, x, y, z);
                if (stopped(f)) {
                    return;
                }
            } else {
                putBoundary(f.bnd, key, roleFor(d));
            }
        }
    }

    private static final int OK = 0;
    private static final int PENDING = 1;
    private static final int STOP = 2;

    /** Handles a door, gate or trapdoor cell met while expanding {@code cell} in direction {@code d}. */
    private int portal(Frame f, long cell, int[] d, int x, int y, int z, long key) {
        Boolean known = decisions.get(key);
        if (known == null) {
            int fx = x + d[0];
            int fy = y + d[1];
            int fz = z + d[2];
            if (!inWorld(fy)) {
                putBoundary(f.bnd, key, roleFor(d));
                return OK;
            }
            if (!probe.loaded(fx, fy, fz)) {
                stats.exteriorUnknown++;
                decide(f, key, d, false);
                known = Boolean.FALSE;
            } else if (probe.portal(fx, fy, fz)) {
                // two doors in a row: this one is a vestibule, the next is judged from inside it
                known = Boolean.TRUE;
            } else if (!probe.passable(fx, fy, fz)) {
                // entered along the panel or from the side of a frame: only a wall from here, no verdict
                putBoundary(f.bnd, key, roleFor(d));
                return OK;
            } else if (visited.contains(HomeRegion.pack(fx, fy, fz))) {
                if (f.main) {
                    stats.interiorKnown++; // a sub-fill looking back at its own door is not a verdict
                }
                decide(f, key, d, true);
                known = Boolean.TRUE;
            } else if (exteriorCells.contains(HomeRegion.pack(fx, fy, fz))) {
                stats.exteriorFill++;
                decide(f, key, d, false);
                known = Boolean.FALSE;
            } else {
                Boolean quick = judge(f, key, d, fx, fy, fz);
                if (quick == null) {
                    return PENDING;
                }
                known = quick;
            }
        }
        if (known) {
            add(f, x, y, z);
            return stopped(f) ? STOP : OK;
        }
        putBoundary(f.bnd, key, roleFor(d));
        return OK;
    }

    /**
     * Layers 1 and 2 of the door test, then starts the sub-fill. Returns FALSE for an outside door, or null when
     * a sub-fill frame was pushed and the verdict is pending.
     */
    private Boolean judge(Frame f, long key, int[] d, int fx, int fy, int fz) {
        if (doorsJudged >= doors.maxDoors) {
            stats.exteriorLimit++;
            decide(f, key, d, false);
            return Boolean.FALSE;
        }
        doorsJudged++;
        if (probe.skyExposed(fx, fy, fz)) {
            stats.exteriorOpenAir++;
            decide(f, key, d, false);
            return Boolean.FALSE;
        }
        int[] r = rays(fx, fy, fz, d);
        double fraction = r[1] == 0 ? 0.0 : r[0] / (double) r[1];
        if (fraction >= doors.exteriorFraction) {
            stats.exteriorRays++;
            decide(f, key, d, false);
            return Boolean.FALSE;
        }
        int cap = doors.probeCells;
        for (Frame a : frames) {
            if (!a.main) {
                cap -= a.cells.size();
            }
        }
        if (cap <= 0 || frames.size() >= MAX_DEPTH) {
            stats.exteriorLimit++;
            decide(f, key, d, false);
            return Boolean.FALSE;
        }
        int tag = fraction <= doors.interiorFraction ? TAG_RAYS : TAG_FILL;
        Frame child = new Frame(false, key, d, cap, tag);
        frames.add(child);
        add(child, fx, fy, fz);
        return null;
    }

    /**
     * Fires rays from the far side of a portal and counts how many escape. Rays pointing back at the door and
     * (unless going down through a hatch) into the floor are skipped: they are always blocked and say nothing.
     *
     * @return {escaped, tested}
     */
    private int[] rays(int fx, int fy, int fz, int[] d) {
        int escaped = 0;
        int tested = 0;
        for (int rx = -1; rx <= 1; rx++) {
            for (int ry = -1; ry <= 1; ry++) {
                for (int rz = -1; rz <= 1; rz++) {
                    if (rx == 0 && ry == 0 && rz == 0) {
                        continue;
                    }
                    if (rx * d[0] + ry * d[1] + rz * d[2] < 0) {
                        continue;
                    }
                    if (ry == -1 && d[1] != -1) {
                        continue;
                    }
                    tested++;
                    if (rayEscapes(fx, fy, fz, rx, ry, rz)) {
                        escaped++;
                    }
                }
            }
        }
        return new int[] {escaped, tested};
    }

    private boolean rayEscapes(int fx, int fy, int fz, int rx, int ry, int rz) {
        for (int i = 1; i <= doors.rayRange; i++) {
            int x = fx + rx * i;
            int y = fy + ry * i;
            int z = fz + rz * i;
            if (y > 255) {
                return true;
            }
            if (y < 0 || !probe.loaded(x, y, z) || probe.portal(x, y, z) || !probe.passable(x, y, z)) {
                return false;
            }
            if (probe.skyExposed(x, y, z)) {
                return true;
            }
        }
        return true;
    }

    /** The sub-fill closed within its budget: its space is part of the home. */
    private void commit(Frame child) {
        frames.remove(frames.size() - 1);
        Frame parent = top();
        for (Map.Entry<Long, Byte> e : child.bnd.entrySet()) {
            putBoundary(parent.bnd, e.getKey(), e.getValue());
        }
        if (!parent.main) {
            parent.cells.addAll(child.cells);
        }
        parent.decided.addAll(child.decided);
        if (child.tag == TAG_RAYS) {
            stats.interiorRays++;
        } else {
            stats.interiorFill++;
        }
        decide(parent, child.portalKey, child.portalDir, true);
        if (!parent.main && parent.cells.size() > parent.cap) {
            fail(parent, "door budget " + parent.cap);
        }
    }

    /** The sub-fill did not close: the portal leads outside and nothing it found joins. */
    private void discard(Frame child) {
        frames.remove(frames.size() - 1);
        Frame parent = top();
        for (long c : child.cells) {
            visited.remove(c);
            // door cells (the sub-fill looks back through its own door) are not outside space
            if (!probe.portal(HomeRegion.unpackX(c), HomeRegion.unpackY(c), HomeRegion.unpackZ(c))) {
                exteriorCells.add(c);
            }
        }
        for (long k : child.decided) {
            decisions.remove(k);
        }
        if (child.failReason != null && child.failReason.startsWith("unloaded")) {
            stats.exteriorUnknown++;
        } else {
            stats.exteriorFill++;
        }
        decide(parent, child.portalKey, child.portalDir, false);
    }

    /** Records a door verdict; the other half of a two-high door gets the same one so it counts once. */
    private void decide(Frame owner, long key, int[] d, boolean interior) {
        record(owner, key, interior);
        if (d != null && d[1] == 0) {
            int x = HomeRegion.unpackX(key);
            int y = HomeRegion.unpackY(key);
            int z = HomeRegion.unpackZ(key);
            for (int dy = -1; dy <= 1; dy += 2) {
                int ty = y + dy;
                if (inWorld(ty)) {
                    long twin = HomeRegion.pack(x, ty, z);
                    if (!decisions.containsKey(twin) && probe.loaded(x, ty, z) && probe.portal(x, ty, z)) {
                        record(owner, twin, interior);
                    }
                }
            }
        }
    }

    private void record(Frame owner, long key, boolean interior) {
        decisions.put(key, interior);
        owner.decided.add(key);
    }

    private static byte roleFor(int[] d) {
        return d[1] == 1 ? HomeRegion.ROLE_ROOF : d[1] == -1 ? HomeRegion.ROLE_FLOOR : HomeRegion.ROLE_WALL;
    }

    private static void putBoundary(Map<Long, Byte> map, long key, byte role) {
        Byte old = map.get(key);
        if (old == null || priority(role) > priority(old)) {
            map.put(key, role);
        }
    }

    private static int priority(byte role) {
        switch (role) {
            case HomeRegion.ROLE_ROOF: return 3;
            case HomeRegion.ROLE_WALL: return 2;
            case HomeRegion.ROLE_FLOOR: return 1;
            default: return 0;
        }
    }

    private static boolean inWorld(int y) {
        return y >= 0 && y <= 255;
    }

    private void finish() {
        done = true;
        if (abortReason != null) {
            result = HomeRegion.box(
                    bedX - fallbackRadius, Math.max(0, bedY - fallbackDown), bedZ - fallbackRadius,
                    bedX + fallbackRadius, Math.min(255, bedY + fallbackUp), bedZ + fallbackRadius,
                    "box: " + abortReason).withPortals(stats);
        } else {
            Map<Long, Byte> boundary = frames.get(0).bnd;
            long[] cells = new long[visited.size()];
            int i = 0;
            for (long c : visited) {
                cells[i++] = c;
            }
            // a door that turned out to be interior was first seen as a wall from another side
            boundary.keySet().removeAll(visited);
            long[] boundaryPositions = new long[boundary.size()];
            byte[] roles = new byte[boundary.size()];
            i = 0;
            for (Map.Entry<Long, Byte> e : boundary.entrySet()) {
                boundaryPositions[i] = e.getKey();
                roles[i] = e.getValue();
                i++;
            }
            result = HomeRegion.filled(cells, boundaryPositions, roles).withPortals(stats);
        }
        frames.clear();
        visited.clear();
        exteriorCells.clear();
        decisions.clear();
    }
}
