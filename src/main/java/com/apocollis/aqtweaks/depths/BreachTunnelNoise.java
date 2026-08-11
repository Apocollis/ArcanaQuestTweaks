package com.apocollis.aqtweaks.depths;

import com.yungnickyoung.minecraft.bettercaves.noise.FastNoise;

/**
 * Shared narrow breach-tube noise (Y≈-25 → 4) so primer / BC companion / chunk reinforce stay in sync.
 * Abs dual-simplex tubes — not product caverns — so the upper band stays mostly solid.
 */
public final class BreachTunnelNoise {

    /** Abs threshold for tube diameter ~4 blocks */
    public static final float TUBE_WIDTH = 0.17f;
    public static final float TUBE_CORE = 0.12f;

    public static final int BOTTOM = -25;
    public static final int TOP = 4;
    /** If tube open here, force-carve through Y=-1…2 */
    public static final int SEAM_PROBE_Y = -2;
    public static final int SEAM_MIN_Y = -1;
    public static final int SEAM_MAX_Y = 2;

    private static FastNoise n1;
    private static FastNoise n2;
    private static boolean initialized = false;

    private BreachTunnelNoise() {}

    public static synchronized void init(long worldSeed) {
        if (initialized) return;
        int seed1 = (int) (worldSeed & 0xFFFF);
        int seed2 = (int) ((worldSeed >> 16) & 0xFFFF);

        n1 = new FastNoise(seed1 + 1111);
        n1.SetNoiseType(FastNoise.NoiseType.Simplex);
        n1.SetFrequency(0.022f);

        n2 = new FastNoise(seed2 + 2222);
        n2.SetNoiseType(FastNoise.NoiseType.Simplex);
        n2.SetFrequency(0.022f);

        initialized = true;
    }

    public static boolean isTubeAt(float a, float b) {
        return Math.abs(a) < TUBE_WIDTH && Math.abs(b) < TUBE_WIDTH;
    }

    public static boolean isCoreAt(float a, float b) {
        return Math.abs(a) < TUBE_CORE && Math.abs(b) < TUBE_CORE;
    }

    public static void sampleColumn(int worldX, int worldZ, float[] out1, float[] out2) {
        int height = TOP - BOTTOM + 1;
        float xz = 1.0f;
        float yComp = 0.85f;
        for (int y = BOTTOM; y <= TOP; y += 4) {
            int idx = y - BOTTOM;
            out1[idx] = n1.GetNoise(worldX * xz, y * yComp, worldZ * xz);
            out2[idx] = n2.GetNoise(worldX * xz, y * yComp, worldZ * xz);
        }
        int last = height - 1;
        if (last % 4 != 0) {
            out1[last] = n1.GetNoise(worldX * xz, TOP * yComp, worldZ * xz);
            out2[last] = n2.GetNoise(worldX * xz, TOP * yComp, worldZ * xz);
        }
        for (int sub = 0; sub < height - 1; sub += 4) {
            int end = Math.min(sub + 4, height - 1);
            float s1 = out1[sub], e1 = out1[end];
            float s2 = out2[sub], e2 = out2[end];
            int span = end - sub;
            for (int i = 1; i < span; ++i) {
                float t = (float) i / (float) span;
                out1[sub + i] = s1 * (1.0f - t) + e1 * t;
                out2[sub + i] = s2 * (1.0f - t) + e2 * t;
            }
        }
    }

    public static int height() {
        return TOP - BOTTOM + 1;
    }

    public static int indexOf(int y) {
        return y - BOTTOM;
    }

    /** True if this column should punch the Y0 seam (tube open at probe or any seam band). */
    public static boolean shouldOpenSeam(float[] v1, float[] v2) {
        int probe = indexOf(SEAM_PROBE_Y);
        if (probe >= 0 && probe < v1.length && isCoreAt(v1[probe], v2[probe])) {
            return true;
        }
        // Also if any seam Y already has a tube
        for (int y = SEAM_MIN_Y; y <= SEAM_MAX_Y; ++y) {
            int idx = indexOf(y);
            if (idx >= 0 && idx < v1.length && isTubeAt(v1[idx], v2[idx])) {
                return true;
            }
        }
        // Continuity from just below seam
        int below = indexOf(-3);
        return below >= 0 && below < v1.length && isTubeAt(v1[below], v2[below]);
    }

    /** Carve this Y as breach tube body or forced seam mouth. */
    public static boolean shouldCarve(int y, float[] v1, float[] v2, boolean forceSeam) {
        if (y < BOTTOM || y > TOP) return false;
        int idx = indexOf(y);
        if (idx < 0 || idx >= v1.length) return false;

        if (y >= SEAM_MIN_Y && y <= SEAM_MAX_Y && forceSeam) {
            return true;
        }
        if (isTubeAt(v1[idx], v2[idx])) {
            return true;
        }
        // ±1 vertical thickness
        for (int dy = -1; dy <= 1; ++dy) {
            int j = idx + dy;
            if (j >= 0 && j < v1.length && isCoreAt(v1[j], v2[j])) {
                return true;
            }
        }
        // Force Y=3–4 open when seam forced so mouths reach into +Y BC band
        return forceSeam && y >= 3 && y <= TOP;
    }
}
