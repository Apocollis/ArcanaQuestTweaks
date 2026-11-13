package com.apocollis.aqtweaks.nether;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Client copy of the ash clouds the server announced: centre, velocity, reach and the coarse occupancy bitmap. */
public final class ClientAshClouds {
    private ClientAshClouds() {}

    public static final class Cloud {
        public final int id;
        public double x;
        public double y;
        public double z;
        public double vx;
        public double vz;
        public double reachH;
        public double reachV;
        public long endTick;
        public int ox;
        public int oy;
        public int oz;
        public int sx;
        public int sy;
        public int sz;
        public byte[] bits;

        Cloud(int id) {
            this.id = id;
        }

        /** True if the block position lies in an occupied coarse cell and inside the reach ellipsoid. */
        public boolean contains(double px, double py, double pz) {
            double dx = (px - x) / reachH;
            double dy = (py - y) / reachV;
            double dz = (pz - z) / reachH;
            if (dx * dx + dy * dy + dz * dz > 1.0D) return false;
            if (bits == null) return false;
            int cx = ((int) Math.floor(px) >> 2) - ox;
            int cy = ((int) Math.floor(py) >> 2) - oy;
            int cz = ((int) Math.floor(pz) >> 2) - oz;
            if (cx < 0 || cy < 0 || cz < 0 || cx >= sx || cy >= sy || cz >= sz) return false;
            int index = cx + sx * (cy + sy * cz);
            return (bits[index >> 3] & (1 << (index & 7))) != 0;
        }
    }

    private static final Map<Integer, Cloud> CLOUDS = new ConcurrentHashMap<>();

    public static void accept(int id, boolean alive, double x, double y, double z, float vx, float vz, float rh,
                              float rv, long endTick, int ox, int oy, int oz, int sx, int sy, int sz, byte[] bits) {
        if (!alive) {
            CLOUDS.remove(id);
            return;
        }
        Cloud c = CLOUDS.computeIfAbsent(id, Cloud::new);
        c.x = x;
        c.y = y;
        c.z = z;
        c.vx = vx;
        c.vz = vz;
        c.reachH = Math.max(1.0D, rh);
        c.reachV = Math.max(1.0D, rv);
        c.endTick = endTick;
        c.ox = ox;
        c.oy = oy;
        c.oz = oz;
        c.sx = sx;
        c.sy = sy;
        c.sz = sz;
        c.bits = bits;
    }

    /** Moves every centre by its velocity; call once per client tick. */
    public static void tick() {
        for (Cloud c : CLOUDS.values()) {
            c.x += c.vx;
            c.z += c.vz;
        }
    }

    public static List<Cloud> all() {
        return new ArrayList<>(CLOUDS.values());
    }

    public static boolean contains(double px, double py, double pz) {
        for (Cloud c : CLOUDS.values()) {
            if (c.contains(px, py, pz)) return true;
        }
        return false;
    }

    public static void clear() {
        CLOUDS.clear();
    }
}
