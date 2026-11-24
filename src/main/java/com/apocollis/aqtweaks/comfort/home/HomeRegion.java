package com.apocollis.aqtweaks.comfort.home;

import java.util.Arrays;

/**
 * Immutable cached region of a player home: the connected passable cells of a build, plus the solid cells that
 * touch them (the walls, floors, roofs and furnishings Comfort scores). A box region (the fallback for open-air
 * or oversized builds) has no cell list: every block inside its bounds is the home.
 *
 * <p>Pure logic, no Minecraft types, so it is unit-tested. Positions are packed the same way as
 * {@code BlockPos.toLong()} (26 bits x, 26 bits z, 12 bits y).
 */
public final class HomeRegion {

    public static final byte ROLE_NONE = 0;
    public static final byte ROLE_ROOF = 1;
    public static final byte ROLE_WALL = 2;
    public static final byte ROLE_WINDOW = 3;
    public static final byte ROLE_FLOOR = 4;

    private final boolean box;
    private final int minX;
    private final int minY;
    private final int minZ;
    private final int maxX;
    private final int maxY;
    private final int maxZ;
    private final long[] cells;
    private final long[] boundary;
    private final byte[] boundaryRoles;
    private final String reason;
    private PortalStats portals = PortalStats.NONE;

    private HomeRegion(boolean box, int minX, int minY, int minZ, int maxX, int maxY, int maxZ,
            long[] cells, long[] boundary, byte[] boundaryRoles, String reason) {
        this.box = box;
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
        this.cells = cells;
        this.boundary = boundary;
        this.boundaryRoles = boundaryRoles;
        this.reason = reason;
    }

    /** Box fallback: every block inside the bounds belongs to the home. */
    public static HomeRegion box(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, String reason) {
        return new HomeRegion(true, minX, minY, minZ, maxX, maxY, maxZ,
                new long[0], new long[0], new byte[0], reason);
    }

    /** Filled region from sorted-or-unsorted cells and boundary entries (sorted here). */
    public static HomeRegion filled(long[] cells, long[] boundaryPositions, byte[] roles) {
        long[] sortedCells = cells.clone();
        Arrays.sort(sortedCells);
        int n = boundaryPositions.length;
        Integer[] order = new Integer[n];
        for (int i = 0; i < n; i++) {
            order[i] = i;
        }
        Arrays.sort(order, (a, b) -> Long.compare(boundaryPositions[a], boundaryPositions[b]));
        long[] sortedBoundary = new long[n];
        byte[] sortedRoles = new byte[n];
        for (int i = 0; i < n; i++) {
            sortedBoundary[i] = boundaryPositions[order[i]];
            sortedRoles[i] = roles[order[i]];
        }
        int x0 = Integer.MAX_VALUE;
        int y0 = Integer.MAX_VALUE;
        int z0 = Integer.MAX_VALUE;
        int x1 = Integer.MIN_VALUE;
        int y1 = Integer.MIN_VALUE;
        int z1 = Integer.MIN_VALUE;
        for (long c : sortedCells) {
            int x = unpackX(c);
            int y = unpackY(c);
            int z = unpackZ(c);
            x0 = Math.min(x0, x);
            y0 = Math.min(y0, y);
            z0 = Math.min(z0, z);
            x1 = Math.max(x1, x);
            y1 = Math.max(y1, y);
            z1 = Math.max(z1, z);
        }
        if (sortedCells.length == 0) {
            x0 = y0 = z0 = 0;
            x1 = y1 = z1 = -1;
        }
        return new HomeRegion(false, x0, y0, z0, x1, y1, z1, sortedCells, sortedBoundary, sortedRoles, "enclosed");
    }

    public static long pack(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (long) (y & 0xFFF);
    }

    public static int unpackX(long p) {
        return (int) (p >> 38);
    }

    public static int unpackY(long p) {
        return (int) (p << 52 >> 52);
    }

    public static int unpackZ(long p) {
        return (int) (p << 26 >> 38);
    }

    /** How this region's doors were judged (counts only, never null). */
    public PortalStats portals() {
        return portals;
    }

    /** Attaches the door counters of the fill that produced this region. */
    public HomeRegion withPortals(PortalStats stats) {
        this.portals = stats == null ? PortalStats.NONE : stats;
        return this;
    }

    /** True when the block is one of the solid blocks touching the home (walls, floors, roof, furnishings). */
    public boolean containsBoundary(int x, int y, int z) {
        return !box && boundary.length > 0 && boundsContain(x, y, z, 1)
                && Arrays.binarySearch(boundary, pack(x, y, z)) >= 0;
    }

    /** True when the block belongs to the home either way: an interior cell, a boundary block, or inside a box. */
    public boolean holds(int x, int y, int z) {
        return contains(x, y, z) || containsBoundary(x, y, z);
    }

    public boolean isBox() {
        return box;
    }

    public String reason() {
        return reason;
    }

    public int minX() { return minX; }
    public int minY() { return minY; }
    public int minZ() { return minZ; }
    public int maxX() { return maxX; }
    public int maxY() { return maxY; }
    public int maxZ() { return maxZ; }

    public int cellCount() {
        return box ? volume() : cells.length;
    }

    public int boundaryCount() {
        return boundary.length;
    }

    public int volume() {
        if (maxX < minX || maxY < minY || maxZ < minZ) {
            return 0;
        }
        return (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
    }

    /** True when the block is inside the home (box: inside the bounds; filled: one of the passable cells). */
    public boolean contains(int x, int y, int z) {
        if (!boundsContain(x, y, z, 0)) {
            return false;
        }
        if (box) {
            return true;
        }
        return Arrays.binarySearch(cells, pack(x, y, z)) >= 0;
    }

    /** Occupancy test for a player: feet block or the block above. */
    public boolean occupies(int feetX, int feetY, int feetZ) {
        return contains(feetX, feetY, feetZ) || contains(feetX, feetY + 1, feetZ);
    }

    /** Cheap bounding-box test, widened by {@code margin} blocks. */
    public boolean boundsContain(int x, int y, int z, int margin) {
        return boxContains(minX, minY, minZ, maxX, maxY, maxZ, x, y, z, margin);
    }

    /**
     * Dirty test for a block change: inside the bounds widened by one, so a change to a wall, floor or
     * furnishing that touches the region counts. A box region is dirty on any change inside its bounds.
     */
    public boolean affectedBy(int x, int y, int z) {
        return boundsContain(x, y, z, box ? 0 : 1);
    }

    public static boolean boxContains(int minX, int minY, int minZ, int maxX, int maxY, int maxZ,
            int x, int y, int z, int margin) {
        return x >= minX - margin && x <= maxX + margin
                && y >= minY - margin && y <= maxY + margin
                && z >= minZ - margin && z <= maxZ + margin;
    }

    /** Packed positions of every passable cell (filled region only; empty for a box). */
    public long[] cells() {
        return cells;
    }

    public long[] boundary() {
        return boundary;
    }

    public byte[] boundaryRoles() {
        return boundaryRoles;
    }
}
