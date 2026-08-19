package com.apocollis.aqtweaks.rtg;

import com.apocollis.aqtweaks.depths.DepthsBiomeUtil;
import com.apocollis.aqtweaks.util.Reflect;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.biome.BiomeProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * Village piece placement: detect water biomes and collect nearby land slots along a street.
 */
public final class VillageLandHelper {

    private VillageLandHelper() {}

    public static boolean isWaterAt(BiomeProvider provider, int x, int z) {
        return DepthsBiomeUtil.isWaterBiome(Reflect.getBiome(provider, x, z));
    }

    public static boolean isWaterAt(Object villageStart, int x, int z) {
        return isWaterAt(Reflect.getVillageStartBiomeProvider(villageStart), x, z);
    }

    public static boolean withinVillageCap(Object villageStart, int x, int z) {
        int[] box = Reflect.getStructureComponentBoxXZ(villageStart);
        if (box == null) return true;
        return Math.abs(x - box[0]) <= 112 && Math.abs(z - box[2]) <= 112;
    }

    /**
     * Nearby land slots, still attached to the incoming street. Does not include the original water position.
     */
    public static List<int[]> landCandidates(Object villageStart, int x, int z, EnumFacing facing, int maxStep) {
        List<int[]> out = new ArrayList<>();
        BiomeProvider provider = Reflect.getVillageStartBiomeProvider(villageStart);
        if (provider == null || maxStep <= 0) return out;

        int backX = 0;
        int backZ = 0;
        int leftX = 0;
        int leftZ = 0;
        int rightX = 0;
        int rightZ = 0;
        if (facing != null) {
            backX = -facing.getXOffset();
            backZ = -facing.getZOffset();
            EnumFacing left = facing.rotateYCCW();
            EnumFacing right = facing.rotateY();
            leftX = left.getXOffset();
            leftZ = left.getZOffset();
            rightX = right.getXOffset();
            rightZ = right.getZOffset();
        }

        addSteps(out, villageStart, provider, x, z, backX, backZ, maxStep, 4);
        int sideMax = Math.min(maxStep, 12);
        addSteps(out, villageStart, provider, x, z, leftX, leftZ, sideMax, 4);
        addSteps(out, villageStart, provider, x, z, rightX, rightZ, sideMax, 4);
        return out;
    }

    private static void addSteps(List<int[]> out, Object villageStart, BiomeProvider provider,
                                 int originX, int originZ, int dirX, int dirZ, int maxStep, int stride) {
        if (dirX == 0 && dirZ == 0) return;
        for (int step = stride; step <= maxStep; step += stride) {
            int nx = originX + dirX * step;
            int nz = originZ + dirZ * step;
            if (!withinVillageCap(villageStart, nx, nz)) continue;
            if (isWaterAt(provider, nx, nz)) continue;
            out.add(new int[] {nx, nz});
        }
    }
}
