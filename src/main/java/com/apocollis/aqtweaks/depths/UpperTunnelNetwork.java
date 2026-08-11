package com.apocollis.aqtweaks.depths;

import com.yungnickyoung.minecraft.bettercaves.noise.FastNoise;
import net.minecraft.util.math.MathHelper;

/**
 * Upper deep tunnels mimicking Better Caves 1.12.2 {@code CaveCarver}:
 * dual-noise high-threshold intersection (Type 1 CubicFractal RigidMulti + Type 2 Simplex),
 * F1/F2 upward y-adjustment for headroom, soft close near DIG_TOP.
 * Chambers are discrete widenings on the worm network.
 * Lower-deep breaches are sparse: only when tunnel floor sits within 1 of the lower ceiling.
 */
public final class UpperTunnelNetwork {

    public static final int SEAM_MIN_Y = -1;
    public static final int SEAM_MAX_Y = 2;
    public static final int SEAM_TOP = 4;
    /** Normal tunnel floor stop — above lower-deep shell (~-27…-23). */
    public static final int DIG_BOTTOM = -22;
    public static final int DIG_TOP = 4;

    private static final int DIG_HEIGHT = DIG_TOP - DIG_BOTTOM + 1;

    // --- Type 1 (worm-like) — BC Cubic defaults ---
    private static final float T1_XZ = 1.6f;
    private static final float T1_Y = 5.0f;
    private static final float T1_THR = 0.95f;
    private static final float T1_F1 = 0.9f;
    private static final float T1_F2 = 0.9f;
    private static final float T1_FREQ = 0.03f;

    // --- Type 2 (open spur) — BC Simplex defaults ---
    private static final float T2_XZ = 0.9f;
    private static final float T2_Y = 2.2f;
    private static final float T2_THR = 0.82f;
    private static final float T2_F1 = 0.95f;
    private static final float T2_F2 = 0.5f;
    private static final float T2_FREQ = 0.025f;

    /** Soft-close depth near DIG_TOP (BC surfaceCutoff-style). */
    private static final int TOP_CUTOFF = 5;

    public static final int CHAMBER_SPACING = 23;
    public static final float CHAMBER_SPAWN_MIN = 0.08f;

    private static FastNoise type1A;
    private static FastNoise type1B;
    private static FastNoise type2A;
    private static FastNoise type2B;
    private static FastNoise chamberSpawn;
    private static FastNoise chamberJitter;
    private static FastNoise chamberShape;
    private static FastNoise chamberSize;
    private static boolean initialized = false;

    private UpperTunnelNetwork() {}

    public static synchronized void init(long worldSeed) {
        if (initialized) return;
        int seed1 = (int) (worldSeed & 0xFFFF);
        int seed2 = (int) ((worldSeed >> 16) & 0xFFFF);

        type1A = new FastNoise(seed1 + 5555);
        type1A.SetNoiseType(FastNoise.NoiseType.CubicFractal);
        type1A.SetFractalType(FastNoise.FractalType.RigidMulti);
        type1A.SetFrequency(T1_FREQ);
        type1A.SetFractalOctaves(1);
        type1A.SetFractalGain(0.3f);

        type1B = new FastNoise(seed2 + 6666);
        type1B.SetNoiseType(FastNoise.NoiseType.CubicFractal);
        type1B.SetFractalType(FastNoise.FractalType.RigidMulti);
        type1B.SetFrequency(T1_FREQ);
        type1B.SetFractalOctaves(1);
        type1B.SetFractalGain(0.3f);

        type2A = new FastNoise(seed1 + 7771);
        type2A.SetNoiseType(FastNoise.NoiseType.Simplex);
        type2A.SetFrequency(T2_FREQ);

        type2B = new FastNoise(seed2 + 8882);
        type2B.SetNoiseType(FastNoise.NoiseType.Simplex);
        type2B.SetFrequency(T2_FREQ);

        chamberSpawn = new FastNoise(seed1 + 5900);
        chamberSpawn.SetNoiseType(FastNoise.NoiseType.Simplex);
        chamberSpawn.SetFrequency(1.0f);

        chamberJitter = new FastNoise(seed2 + 5910);
        chamberJitter.SetNoiseType(FastNoise.NoiseType.Simplex);
        chamberJitter.SetFrequency(1.0f);

        chamberShape = new FastNoise(seed1 + 5920);
        chamberShape.SetNoiseType(FastNoise.NoiseType.Simplex);
        chamberShape.SetFrequency(1.0f);

        chamberSize = new FastNoise(seed2 + 5930);
        chamberSize.SetNoiseType(FastNoise.NoiseType.Simplex);
        chamberSize.SetFrequency(1.0f);

        initialized = true;
    }

    private static float thresholdAt(int y, float baseThr) {
        int boundary = DIG_TOP - TOP_CUTOFF;
        if (y < boundary || DIG_TOP == boundary) return baseThr;
        float t = (float) (y - boundary) / (float) (DIG_TOP - boundary);
        return baseThr * (1.0f + 0.3f * t);
    }

    /**
     * BC CaveCarver dig for one dual-noise system: sample → y-adjust upward → dig if both ≥ thr.
     */
    private static boolean[] digSystem(FastNoise n1, FastNoise n2, float xzComp, float yComp,
                                       float baseThr, float f1, float f2, int worldX, int worldZ) {
        float[] a = new float[DIG_HEIGHT];
        float[] b = new float[DIG_HEIGHT];
        for (int y = DIG_BOTTOM; y <= DIG_TOP; ++y) {
            int i = y - DIG_BOTTOM;
            a[i] = n1.GetNoise(worldX * xzComp, y * yComp, worldZ * xzComp);
            b[i] = n2.GetNoise(worldX * xzComp, y * yComp, worldZ * xzComp);
        }

        // preprocess: top → bottom; when cell digs, blend into cells above (headroom)
        for (int y = DIG_TOP; y >= DIG_BOTTOM; --y) {
            int i = y - DIG_BOTTOM;
            float thr = thresholdAt(y, baseThr);
            if (a[i] < thr || b[i] < thr) continue;

            if (y + 1 <= DIG_TOP) {
                int j = i + 1;
                a[j] = (1.0f - f1) * a[j] + f1 * a[i];
                b[j] = (1.0f - f1) * b[j] + f1 * b[i];
            }
            if (y + 2 <= DIG_TOP) {
                int j = i + 2;
                a[j] = (1.0f - f2) * a[j] + f2 * a[i];
                b[j] = (1.0f - f2) * b[j] + f2 * b[i];
            }
        }

        boolean[] dig = new boolean[DIG_HEIGHT];
        for (int y = DIG_BOTTOM; y <= DIG_TOP; ++y) {
            int i = y - DIG_BOTTOM;
            float thr = thresholdAt(y, baseThr);
            dig[i] = a[i] >= thr && b[i] >= thr;
        }
        return dig;
    }

    private static boolean rawDigAt(FastNoise n1, FastNoise n2, float xzComp, float yComp,
                                    float baseThr, int worldX, int y, int worldZ) {
        if (y < DIG_BOTTOM || y > DIG_TOP) return false;
        float thr = thresholdAt(y, baseThr);
        float a = n1.GetNoise(worldX * xzComp, y * yComp, worldZ * xzComp);
        float b = n2.GetNoise(worldX * xzComp, y * yComp, worldZ * xzComp);
        return a >= thr && b >= thr;
    }

    /** True if this block is inside a tunnel dig (either Type 1 or Type 2), raw (no y-adjust). */
    public static boolean wormDigCore(int worldX, int y, int worldZ) {
        if (rawDigAt(type1A, type1B, T1_XZ, T1_Y, T1_THR, worldX, y, worldZ)) return true;
        return rawDigAt(type2A, type2B, T2_XZ, T2_Y, T2_THR, worldX, y, worldZ);
    }

    public static boolean carveTunnelAt(int worldX, int worldZ, int y) {
        return forColumn(worldX, worldZ).carveTunnelAt(y);
    }

    public static boolean shouldOpenSeam(int worldX, int worldZ) {
        return forColumn(worldX, worldZ).shouldOpenSeam();
    }

    /** @deprecated use {@link ColumnDigCache#shouldBreachLower(int)} with ceilY */
    @Deprecated
    public static boolean breachesLower(int worldX, int worldZ) {
        return false;
    }

    public static boolean chamberBounds(int worldX, int worldZ, int[] outFloorTop) {
        return forColumn(worldX, worldZ).chamberBounds(outFloorTop);
    }

    public static boolean carveUpperAt(int worldX, int worldZ, int y) {
        return forColumn(worldX, worldZ).carveUpperAt(y);
    }

    /** Build dig/chamber/seam flags once per column (hot path for primer / seam). */
    public static ColumnDigCache forColumn(int worldX, int worldZ) {
        boolean[] t1 = digSystem(type1A, type1B, T1_XZ, T1_Y, T1_THR, T1_F1, T1_F2, worldX, worldZ);
        boolean[] t2 = digSystem(type2A, type2B, T2_XZ, T2_Y, T2_THR, T2_F1, T2_F2, worldX, worldZ);

        boolean[] tunnel = new boolean[DIG_HEIGHT];
        for (int i = 0; i < DIG_HEIGHT; ++i) {
            tunnel[i] = t1[i] || t2[i];
        }

        int[] ft = new int[2];
        boolean hasChamber = chamberBoundsRaw(worldX, worldZ, ft, true);
        int chamberFloor = hasChamber ? ft[0] : 0;
        int chamberTop = hasChamber ? ft[1] : Integer.MIN_VALUE;

        // Chamber connectors: ±1 neighbor raw dig (exits without hollowing the room)
        if (hasChamber) {
            for (int y = DIG_BOTTOM; y <= DIG_TOP; ++y) {
                int i = y - DIG_BOTTOM;
                if (tunnel[i]) continue;
                if (y < chamberFloor - 1 || y > chamberTop + 1) continue;
                outer:
                for (int dx = -1; dx <= 1; ++dx) {
                    for (int dz = -1; dz <= 1; ++dz) {
                        if (dx == 0 && dz == 0) continue;
                        if (wormDigCore(worldX + dx, y, worldZ + dz)
                                || (y - 1 >= DIG_BOTTOM && wormDigCore(worldX + dx, y - 1, worldZ + dz))) {
                            tunnel[i] = true;
                            break outer;
                        }
                    }
                }
            }
        }

        int tunnelFloorY = Integer.MAX_VALUE;
        for (int y = DIG_BOTTOM; y <= DIG_TOP; ++y) {
            if (tunnel[y - DIG_BOTTOM]) {
                tunnelFloorY = y;
                break;
            }
        }
        // Chamber floor can be the walking surface when present
        if (hasChamber && chamberFloor < tunnelFloorY) {
            tunnelFloorY = chamberFloor;
        }

        boolean openSeam = false;
        for (int y = -2; y <= SEAM_TOP; ++y) {
            if (y >= DIG_BOTTOM && y <= DIG_TOP && tunnel[y - DIG_BOTTOM]) {
                openSeam = true;
                break;
            }
        }

        return new ColumnDigCache(tunnel, hasChamber, chamberFloor, chamberTop, openSeam, tunnelFloorY);
    }

    private static boolean columnHasWorm(int worldX, int worldZ) {
        for (int y = DIG_BOTTOM; y <= DIG_TOP; y += 2) {
            if (wormDigCore(worldX, y, worldZ)) return true;
        }
        return false;
    }

    private static boolean chamberBoundsRaw(int worldX, int worldZ, int[] outFloorTop, boolean requireWorm) {
        int spacing = CHAMBER_SPACING;
        int cellX = Math.floorDiv(worldX, spacing);
        int cellZ = Math.floorDiv(worldZ, spacing);
        boolean found = false;
        int bestFloor = 0;
        int bestTop = Integer.MIN_VALUE;

        for (int dx = -1; dx <= 1; ++dx) {
            for (int dz = -1; dz <= 1; ++dz) {
                int cx = cellX + dx;
                int cz = cellZ + dz;
                if (chamberSpawn.GetNoise(cx * 17.1f, cz * 29.3f) < CHAMBER_SPAWN_MIN) continue;

                float jx = chamberJitter.GetNoise(cx * 11.3f, cz * 19.7f);
                float jz = chamberJitter.GetNoise(cx * 23.1f + 40.0f, cz * 13.9f);
                int centerX = Math.round(cx * spacing + spacing * 0.5f + jx * (spacing * 0.18f));
                int centerZ = Math.round(cz * spacing + spacing * 0.5f + jz * (spacing * 0.18f));

                if (requireWorm && !columnHasWorm(centerX, centerZ)) continue;

                float sizeN = chamberSize.GetNoise(cx * 7.7f, cz * 9.1f) * 0.5f + 0.5f;
                float half = 4.0f + sizeN * 2.0f; // diameter 8–12
                boolean square = chamberShape.GetNoise(cx * 5.3f, cz * 15.7f) >= 0.0f;

                float dxw = worldX - centerX;
                float dzw = worldZ - centerZ;
                float norm;
                if (square) {
                    float ax = Math.abs(dxw) / half;
                    float az = Math.abs(dzw) / half;
                    if (ax > 1.0f || az > 1.0f) continue;
                    norm = Math.max(ax, az);
                } else {
                    float dist = MathHelper.sqrt(dxw * dxw + dzw * dzw);
                    if (dist > half) continue;
                    norm = dist / half;
                }

                int chamberFloor = -16;
                for (int y = -20; y <= -8; ++y) {
                    if (wormDigCore(centerX, y, centerZ)) {
                        chamberFloor = y;
                        break;
                    }
                }
                chamberFloor = MathHelper.clamp(chamberFloor, -20, -8);

                float hFrac = (float) Math.sqrt(Math.max(0.0, 1.0 - norm * norm));
                int maxH = MathHelper.clamp(5 + Math.round(3.0f * hFrac), 4, 8);
                int top = Math.min(chamberFloor + maxH - 1, -5);

                if (!found || top > bestTop) {
                    found = true;
                    bestFloor = chamberFloor;
                    bestTop = top;
                }
            }
        }

        if (!found) return false;
        outFloorTop[0] = bestFloor;
        outFloorTop[1] = bestTop;
        return true;
    }

    /** Per-column dig/chamber/seam cache for primer and seam hot paths. */
    public static final class ColumnDigCache {
        private final boolean[] tunnel;
        private final boolean hasChamber;
        private final int chamberFloor;
        private final int chamberTop;
        private final boolean openSeam;
        private final int tunnelFloorY;

        private ColumnDigCache(boolean[] tunnel, boolean hasChamber, int chamberFloor, int chamberTop,
                               boolean openSeam, int tunnelFloorY) {
            this.tunnel = tunnel;
            this.hasChamber = hasChamber;
            this.chamberFloor = chamberFloor;
            this.chamberTop = chamberTop;
            this.openSeam = openSeam;
            this.tunnelFloorY = tunnelFloorY;
        }

        public boolean carveTunnelAt(int y) {
            if (y < DIG_BOTTOM || y > DIG_TOP) return false;
            return tunnel[y - DIG_BOTTOM];
        }

        public boolean carveUpperAt(int y) {
            if (y < DIG_BOTTOM || y > DIG_TOP) return false;
            if (carveTunnelAt(y)) return true;
            return hasChamber && y >= chamberFloor && y <= chamberTop;
        }

        public boolean shouldOpenSeam() {
            return openSeam;
        }

        /** Lowest tunnel/chamber floor Y, or Integer.MAX_VALUE if none. */
        public int tunnelFloorY() {
            return tunnelFloorY;
        }

        /**
         * Sparse lower breach: tunnel floor sits on or 1 block above the lower-deep ceiling.
         */
        public boolean shouldBreachLower(int ceilY) {
            if (tunnelFloorY == Integer.MAX_VALUE) return false;
            return tunnelFloorY >= ceilY && tunnelFloorY <= ceilY + 1;
        }

        /** Vertical shaft Y range through the shell into lower deep (inclusive). */
        public boolean isBreachShaft(int y, int ceilY) {
            if (!shouldBreachLower(ceilY)) return false;
            int shaftBottom = ceilY - 2;
            return y >= shaftBottom && y <= tunnelFloorY;
        }

        public boolean chamberBounds(int[] outFloorTop) {
            if (!hasChamber) return false;
            outFloorTop[0] = chamberFloor;
            outFloorTop[1] = chamberTop;
            return true;
        }
    }
}
