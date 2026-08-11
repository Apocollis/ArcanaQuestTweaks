package com.apocollis.aqtweaks.depths;

import com.yungnickyoung.minecraft.bettercaves.noise.FastNoise;
import net.minecraft.util.math.MathHelper;

/**
 * Upper deep tunnels in Better Caves surface-cave style:
 * dual-simplex 3D worms (intersect) with xz/y compression, y-adjustment headroom,
 * two worm systems OR'd for winding / branching / interconnecting tubes from ~Y4 to ~-25.
 * Chambers are discrete widenings attached to the worm network.
 */
public final class UpperTunnelNetwork {

    public static final int SEAM_MAX_Y = 2;
    public static final int SEAM_TOP = 4;
    public static final int DIG_BOTTOM = -25;
    public static final int DIG_TOP = 4;

    /** BC-like compressions (vertical higher → steeper vertical play) */
    private static final float XZ_COMP = 1.0f;
    private static final float Y_COMP = 2.5f;
    /** Abs dual-noise tube width (~3–4 block worms) */
    private static final float WORM_WIDTH = 0.115f;
    private static final float WORM_WIDTH_SPUR = 0.09f;
    /** Headroom: only +1 above core, same tightness as primary */
    private static final float Y_ADJUST_WIDTH = 0.115f;

    public static final int CHAMBER_SPACING = 23;
    public static final float CHAMBER_SPAWN_MIN = 0.08f;

    private static FastNoise wormA1;
    private static FastNoise wormA2;
    private static FastNoise wormB1;
    private static FastNoise wormB2;
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

        // System A — primary winding worms (BC surface-cave style)
        wormA1 = new FastNoise(seed1 + 5555);
        wormA1.SetNoiseType(FastNoise.NoiseType.Simplex);
        wormA1.SetFrequency(0.025f);

        wormA2 = new FastNoise(seed2 + 6666);
        wormA2.SetNoiseType(FastNoise.NoiseType.Simplex);
        wormA2.SetFrequency(0.025f);

        // System B — second network for branches / interconnects
        wormB1 = new FastNoise(seed1 + 7771);
        wormB1.SetNoiseType(FastNoise.NoiseType.Simplex);
        wormB1.SetFrequency(0.028f);

        wormB2 = new FastNoise(seed2 + 8882);
        wormB2.SetNoiseType(FastNoise.NoiseType.Simplex);
        wormB2.SetFrequency(0.028f);

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

    private static boolean wormCoreAt(FastNoise n1, FastNoise n2, int x, int y, int z, float width) {
        float a = n1.GetNoise(x * XZ_COMP, y * Y_COMP, z * XZ_COMP);
        float b = n2.GetNoise(x * XZ_COMP, y * Y_COMP, z * XZ_COMP);
        return Math.abs(a) < width && Math.abs(b) < width;
    }

    /** True if this block is inside a BC-style worm dig (either system). */
    public static boolean wormDigCore(int worldX, int y, int worldZ) {
        if (y < DIG_BOTTOM || y > DIG_TOP) return false;
        if (wormCoreAt(wormA1, wormA2, worldX, y, worldZ, WORM_WIDTH)) return true;
        return wormCoreAt(wormB1, wormB2, worldX, y, worldZ, WORM_WIDTH_SPUR);
    }

    /**
     * Worm dig including BC-like y-adjustment (headroom above a core dig).
     */
    public static boolean carveTunnelAt(int worldX, int worldZ, int y) {
        if (y < DIG_BOTTOM || y > DIG_TOP) return false;
        if (wormDigCore(worldX, y, worldZ)) return true;

        // y-adjustment: only +1 block headroom above a core dig
        if (y - 1 >= DIG_BOTTOM && wormDigCore(worldX, y - 1, worldZ)) {
            return wormCoreAt(wormA1, wormA2, worldX, y - 1, worldZ, Y_ADJUST_WIDTH)
                    || wormCoreAt(wormB1, wormB2, worldX, y - 1, worldZ, Y_ADJUST_WIDTH);
        }

        // Chamber connector: ±1 neighbor only (exits without hollowing the room)
        if (isInChamberFootprint(worldX, worldZ)) {
            for (int dx = -1; dx <= 1; ++dx) {
                for (int dz = -1; dz <= 1; ++dz) {
                    if (dx == 0 && dz == 0) continue;
                    if (wormDigCore(worldX + dx, y, worldZ + dz)) return true;
                    if (y - 1 >= DIG_BOTTOM && wormDigCore(worldX + dx, y - 1, worldZ + dz)) return true;
                }
            }
        }
        return false;
    }

    /** Column has worm opening near the Y0 seam. */
    public static boolean shouldOpenSeam(int worldX, int worldZ) {
        for (int y = -2; y <= SEAM_TOP; ++y) {
            if (carveTunnelAt(worldX, worldZ, y)) return true;
        }
        return false;
    }

    /** Column has worm cutting the lower-deep ceiling band. */
    public static boolean breachesLower(int worldX, int worldZ) {
        for (int y = -26; y <= -22; ++y) {
            if (carveTunnelAt(worldX, worldZ, y)) return true;
        }
        return false;
    }

    private static boolean columnHasWorm(int worldX, int worldZ) {
        for (int y = DIG_BOTTOM; y <= DIG_TOP; y += 2) {
            if (wormDigCore(worldX, y, worldZ)) return true;
        }
        return false;
    }

    private static boolean isInChamberFootprint(int worldX, int worldZ) {
        int[] ft = new int[2];
        return chamberBoundsRaw(worldX, worldZ, ft, true);
    }

    /**
     * Chamber bounds; requireWorm means center must sit on the worm network.
     */
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

                // Flat floor near local worm mid-band
                int chamberFloor = -16;
                for (int y = -22; y <= -8; ++y) {
                    if (wormDigCore(centerX, y, centerZ)) {
                        chamberFloor = y;
                        break;
                    }
                }
                chamberFloor = MathHelper.clamp(chamberFloor, -22, -8);

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

    public static boolean chamberBounds(int worldX, int worldZ, int[] outFloorTop) {
        return chamberBoundsRaw(worldX, worldZ, outFloorTop, true);
    }

    public static boolean carveUpperAt(int worldX, int worldZ, int y) {
        if (y < DIG_BOTTOM - 1 || y > DIG_TOP) return false;
        if (carveTunnelAt(worldX, worldZ, y)) return true;

        int[] ft = new int[2];
        if (!chamberBounds(worldX, worldZ, ft)) return false;
        return y >= ft[0] && y <= ft[1];
    }
}
