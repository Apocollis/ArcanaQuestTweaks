package com.apocollis.aqtweaks.nether;

import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

/** Server state of one ash cloud: a drifting centre, a reach, and the exact set of open cells it occupies. */
public final class AshCloud {
    public final int id;
    public double x;
    public double y;
    public double z;
    public double angle;
    public double vx;
    public double vz;
    public final long startTick;
    public final long endTick;
    public LongOpenHashSet occupied = new LongOpenHashSet();
    public AshCloudField fill;
    public long lastFillStart;
    /** Last packet sent for this cloud and the world tick it was sent, so an unchanged cloud is not resent. */
    public com.apocollis.aqtweaks.network.PacketAshCloud lastSent;
    public long lastSentTick;

    public AshCloud(int id, double x, double y, double z, double angle, long startTick, long endTick) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.z = z;
        this.angle = angle;
        this.startTick = startTick;
        this.endTick = endTick;
    }

    /** True once the storm is active: an unarmed cloud was filled early (in the last seconds of the warning) and is not yet announced or harmful. */
    public boolean armed;

    /** 0.1..1: arrives at 85 percent reach and fills out over 3 s (the warning is the build-up), shrinks over the last 20 s. */
    public double scale(long now) {
        double grow = Math.min(1.0D, 0.85D + 0.15D * Math.max(0L, now - startTick) / 60.0D);
        double fade = Math.min(1.0D, (endTick - now) / 400.0D);
        return Math.max(0.1D, Math.min(grow, fade));
    }

    public boolean contains(int bx, int by, int bz) {
        return occupied.contains(AshCloudField.pack(bx, by, bz));
    }

    /** Coarse 4-block occupancy for the client: origin cell, size, and one bit per cell. Null when empty. */
    public int[] bounds() {
        if (occupied.isEmpty()) return null;
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        LongIterator it = occupied.iterator();
        while (it.hasNext()) {
            long p = it.nextLong();
            int cx = AshCloudField.unpackX(p) >> 2;
            int cy = AshCloudField.unpackY(p) >> 2;
            int cz = AshCloudField.unpackZ(p) >> 2;
            if (cx < minX) minX = cx;
            if (cx > maxX) maxX = cx;
            if (cy < minY) minY = cy;
            if (cy > maxY) maxY = cy;
            if (cz < minZ) minZ = cz;
            if (cz > maxZ) maxZ = cz;
        }
        return new int[] {minX, minY, minZ, maxX - minX + 1, maxY - minY + 1, maxZ - minZ + 1};
    }

    public byte[] bitmap(int[] b) {
        int sx = b[3];
        int sy = b[4];
        int sz = b[5];
        byte[] bits = new byte[(sx * sy * sz + 7) / 8];
        LongIterator it = occupied.iterator();
        while (it.hasNext()) {
            long p = it.nextLong();
            int cx = (AshCloudField.unpackX(p) >> 2) - b[0];
            int cy = (AshCloudField.unpackY(p) >> 2) - b[1];
            int cz = (AshCloudField.unpackZ(p) >> 2) - b[2];
            int index = cx + sx * (cy + sy * cz);
            bits[index >> 3] |= (byte) (1 << (index & 7));
        }
        return bits;
    }
}
