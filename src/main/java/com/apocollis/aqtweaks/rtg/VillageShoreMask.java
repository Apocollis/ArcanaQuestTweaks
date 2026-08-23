package com.apocollis.aqtweaks.rtg;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.util.Reflect;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeProvider;

import java.util.Collections;
import java.util.List;

/**
 * Hard-pad occupancy after 1-block hole fill and morphological opening, plus 8-connected rim.
 * Flatten and seal must use the same instance inputs (boxes + pads) for one chunk.
 */
public final class VillageShoreMask {

    private final int originX;
    private final int originZ;
    private final int dim;
    private final boolean[] plate;

    private VillageShoreMask(int originX, int originZ, int dim, boolean[] plate) {
        this.originX = originX;
        this.originZ = originZ;
        this.dim = dim;
        this.plate = plate;
    }

    public static VillageShoreMask build(BiomeProvider provider, int chunkStartX, int chunkStartZ,
                                         List<int[]> landBoxes, List<int[]> shrineBoxes,
                                         int componentPad, int shrinePad) {
        List<int[]> land = landBoxes != null ? landBoxes : Collections.emptyList();
        List<int[]> shrine = shrineBoxes != null ? shrineBoxes : Collections.emptyList();
        boolean smooth = ArcanaQuestTweaksConfig.RtgModuleConfig.surface.villageShoreSmooth;
        int radius = Math.max(0, Math.min(2, ArcanaQuestTweaksConfig.RtgModuleConfig.surface.villageShoreSmoothRadius));
        boolean closeOcean = ArcanaQuestTweaksConfig.RtgModuleConfig.surface.villageShoreCloseOcean;
        int halo = Math.max(1, smooth ? 2 * Math.max(1, radius) : 1);
        int originX = chunkStartX - halo;
        int originZ = chunkStartZ - halo;
        int dim = 16 + 2 * halo;
        int n = dim * dim;
        boolean[] raw = new boolean[n];
        boolean[] protect = new boolean[n];
        boolean[] inPad = new boolean[n];
        boolean[] never = new boolean[n];
        for (int iz = 0; iz < dim; iz++) {
            int wz = originZ + iz;
            for (int ix = 0; ix < dim; ix++) {
                int wx = originX + ix;
                int i = ix + iz * dim;
                Biome biome = Reflect.getBiome(provider, wx, wz);
                never[i] = VillageLandHelper.isNeverRaiseBiome(biome);
                double landDist = nearestDist(wx, wz, land);
                double shrineDist = nearestDist(wx, wz, shrine);
                boolean hard = (landDist <= componentPad && landDist < Double.MAX_VALUE)
                        || (shrineDist <= shrinePad && shrineDist < Double.MAX_VALUE);
                inPad[i] = hard;
                boolean interior = landDist <= 0.0 || shrineDist <= 0.0;
                raw[i] = hard && !never[i];
                protect[i] = interior && !never[i];
            }
        }
        boolean[] pre = raw;
        if (closeOcean) {
            pre = holeFill(raw, inPad, never, dim);
        }
        boolean[] out = pre;
        if (smooth && radius > 0) {
            out = open(pre, protect, dim, radius);
        }
        for (int i = 0; i < n; i++) {
            if (protect[i] && pre[i]) {
                out[i] = true;
            }
        }
        return new VillageShoreMask(originX, originZ, dim, out);
    }

    public boolean plated(int worldX, int worldZ) {
        int ix = worldX - originX;
        int iz = worldZ - originZ;
        if (ix < 0 || iz < 0 || ix >= dim || iz >= dim) {
            return false;
        }
        return plate[ix + iz * dim];
    }

    public boolean rim(int worldX, int worldZ) {
        if (!plated(worldX, worldZ)) {
            return false;
        }
        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                if (!plated(worldX + dx, worldZ + dz)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean[] holeFill(boolean[] raw, boolean[] inPad, boolean[] never, int dim) {
        boolean[] filled = raw.clone();
        for (int iz = 0; iz < dim; iz++) {
            for (int ix = 0; ix < dim; ix++) {
                int i = ix + iz * dim;
                if (raw[i] || !inPad[i] || !never[i]) {
                    continue;
                }
                if (enclosedCardinal(raw, dim, ix, iz) || landNeighbors8(raw, dim, ix, iz) >= 7) {
                    filled[i] = true;
                }
            }
        }
        return filled;
    }

    private static boolean enclosedCardinal(boolean[] raw, int dim, int ix, int iz) {
        return platedAt(raw, dim, ix + 1, iz)
                && platedAt(raw, dim, ix - 1, iz)
                && platedAt(raw, dim, ix, iz + 1)
                && platedAt(raw, dim, ix, iz - 1);
    }

    private static int landNeighbors8(boolean[] raw, int dim, int ix, int iz) {
        int n = 0;
        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                if (platedAt(raw, dim, ix + dx, iz + dz)) {
                    n++;
                }
            }
        }
        return n;
    }

    private static boolean platedAt(boolean[] raw, int dim, int ix, int iz) {
        if (ix < 0 || iz < 0 || ix >= dim || iz >= dim) {
            return false;
        }
        return raw[ix + iz * dim];
    }

    private static boolean[] open(boolean[] pre, boolean[] protect, int dim, int radius) {
        boolean[] eroded = new boolean[pre.length];
        for (int iz = 0; iz < dim; iz++) {
            for (int ix = 0; ix < dim; ix++) {
                int i = ix + iz * dim;
                if (protect[i] && pre[i]) {
                    eroded[i] = true;
                    continue;
                }
                eroded[i] = chebyshevAll(pre, dim, ix, iz, radius);
            }
        }
        boolean[] dilated = new boolean[pre.length];
        for (int iz = 0; iz < dim; iz++) {
            for (int ix = 0; ix < dim; ix++) {
                int i = ix + iz * dim;
                dilated[i] = pre[i] && chebyshevAny(eroded, dim, ix, iz, radius);
            }
        }
        return dilated;
    }

    private static boolean chebyshevAll(boolean[] mask, int dim, int ix, int iz, int radius) {
        for (int dz = -radius; dz <= radius; dz++) {
            for (int dx = -radius; dx <= radius; dx++) {
                if (!platedAt(mask, dim, ix + dx, iz + dz)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean chebyshevAny(boolean[] mask, int dim, int ix, int iz, int radius) {
        for (int dz = -radius; dz <= radius; dz++) {
            for (int dx = -radius; dx <= radius; dx++) {
                if (platedAt(mask, dim, ix + dx, iz + dz)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static double nearestDist(int x, int z, List<int[]> boxes) {
        double best = Double.MAX_VALUE;
        for (int[] box : boxes) {
            double dist = distanceToBoxXZ(x, z, box[0], box[1], box[2], box[3]);
            if (dist < best) {
                best = dist;
            }
        }
        return best;
    }

    private static double distanceToBoxXZ(int x, int z, int minX, int maxX, int minZ, int maxZ) {
        int dx = 0;
        if (x < minX) dx = minX - x;
        else if (x > maxX) dx = x - maxX;
        int dz = 0;
        if (z < minZ) dz = minZ - z;
        else if (z > maxZ) dz = z - maxZ;
        if (dx == 0 && dz == 0) return 0.0;
        return Math.sqrt((double) dx * dx + (double) dz * dz);
    }
}
