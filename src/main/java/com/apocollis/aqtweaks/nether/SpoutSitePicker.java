package com.apocollis.aqtweaks.nether;

/** Pure acceptance rules for a spout site (the world adapter finds the lava). No Minecraft types. */
public final class SpoutSitePicker {
    private SpoutSitePicker() {}

    /** Horizontal distance from the player must be within [minDistance, range]. */
    public static boolean distanceOk(double dx, double dz, double minDistance, double range) {
        double d2 = dx * dx + dz * dz;
        return d2 >= minDistance * minDistance && d2 <= range * range;
    }

    /** A site must be at least {@code separation} blocks from every other active site. */
    public static boolean farFromOthers(int x, int y, int z, int[][] others, double separation) {
        double s2 = separation * separation;
        for (int[] o : others) {
            double dx = x - o[0];
            double dy = y - o[1];
            double dz = z - o[2];
            if (dx * dx + dy * dy + dz * dz < s2) return false;
        }
        return true;
    }
}
