package com.apocollis.aqtweaks.rtg;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.depths.DepthsBiomeUtil;
import com.apocollis.aqtweaks.util.Reflect;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraftforge.common.BiomeDictionary;
import rtg.world.gen.ChunkGeneratorRTG;
import rtg.world.gen.ChunkLandscape;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Village placement: wet-column tests, coastal start veto, and land retry slots.
 */
public final class VillageLandHelper {

    public static final float STRONG_RIVER = 0.7F;

    private static final ThreadLocal<Deque<World>> WORLDS = ThreadLocal.withInitial(ArrayDeque::new);
    private static final ThreadLocal<Integer> SAMPLING = ThreadLocal.withInitial(() -> 0);

    private VillageLandHelper() {}

    public static void pushWorld(World world) {
        WORLDS.get().push(world);
    }

    public static void popWorld() {
        Deque<World> stack = WORLDS.get();
        if (!stack.isEmpty()) {
            stack.pop();
        }
    }

    public static World currentWorld() {
        Deque<World> stack = WORLDS.get();
        return stack.isEmpty() ? null : stack.peek();
    }

    public static boolean isSamplingLandscape() {
        return SAMPLING.get() > 0;
    }

    public static boolean isWaterAt(BiomeProvider provider, int x, int z) {
        return DepthsBiomeUtil.isWaterBiome(Reflect.getBiome(provider, x, z));
    }

    public static boolean isWaterAt(Object villageStart, int x, int z) {
        if (isWaterAt(Reflect.getVillageStartBiomeProvider(villageStart), x, z)) {
            return true;
        }
        World world = currentWorld();
        if (world == null) {
            world = Reflect.getVillageStartWorld(villageStart);
        }
        return isRtgWetColumn(world, x, z);
    }

    public static boolean villageStartAllowed(World world, int chunkX, int chunkZ) {
        if (world == null) return true;
        int wellX = chunkX * 16 + 2;
        int wellZ = chunkZ * 16 + 2;
        BiomeProvider provider = world.getBiomeProvider();
        if (isWaterAt(provider, wellX, wellZ)) {
            return false;
        }
        int buffer = Math.max(0, ArcanaQuestTweaksConfig.RtgModuleConfig.surface.villageCoastBuffer);
        for (int dx = -buffer; dx <= buffer; dx += 8) {
            for (int dz = -buffer; dz <= buffer; dz += 8) {
                if (isDeepOcean(Reflect.getBiome(provider, wellX + dx, wellZ + dz))) {
                    return false;
                }
            }
        }
        Float height = sampleRtgHeight(world, wellX, wellZ);
        if (height != null && height < minWellHeight()) {
            return false;
        }
        return true;
    }

    public static boolean isWetColumn(BiomeProvider provider, ChunkLandscape landscape, int index, int worldX, int worldZ) {
        if (isWaterAt(provider, worldX, worldZ)) {
            return true;
        }
        if (landscape == null) return false;
        if (landscape.river != null && index >= 0 && index < landscape.river.length
                && Math.abs(landscape.river[index]) > STRONG_RIVER) {
            return true;
        }
        if (landscape.noise != null && index >= 0 && index < landscape.noise.length
                && landscape.noise[index] < minWellHeight()) {
            return true;
        }
        return false;
    }

    public static boolean isDeepOcean(Biome biome) {
        if (biome == null) return false;
        try {
            String name = biome.getRegistryName() != null ? biome.getRegistryName().toString().toLowerCase() : "";
            if (name.contains("deep_ocean") || name.contains("deepocean")) {
                return true;
            }
            boolean ocean = BiomeDictionary.hasType(biome, BiomeDictionary.Type.OCEAN)
                    || name.contains("ocean");
            return ocean && name.contains("deep");
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean isAabbWet(Object villageStart, Object component) {
        int[] box = Reflect.getStructureComponentBoxXZ(component);
        if (box == null) return false;
        int stride = 4;
        for (int x = box[0]; x <= box[1]; x += stride) {
            for (int z = box[2]; z <= box[3]; z += stride) {
                if (isWaterAt(villageStart, x, z)) return true;
            }
        }
        if (isWaterAt(villageStart, box[1], box[3])) return true;
        return false;
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

        addSteps(out, villageStart, x, z, backX, backZ, maxStep, 4);
        int sideMax = Math.min(maxStep, 12);
        addSteps(out, villageStart, x, z, leftX, leftZ, sideMax, 4);
        addSteps(out, villageStart, x, z, rightX, rightZ, sideMax, 4);
        return out;
    }

    private static void addSteps(List<int[]> out, Object villageStart,
                                 int originX, int originZ, int dirX, int dirZ, int maxStep, int stride) {
        if (dirX == 0 && dirZ == 0) return;
        for (int step = stride; step <= maxStep; step += stride) {
            int nx = originX + dirX * step;
            int nz = originZ + dirZ * step;
            if (!withinVillageCap(villageStart, nx, nz)) continue;
            if (isWaterAt(villageStart, nx, nz)) continue;
            out.add(new int[] {nx, nz});
        }
    }

    private static boolean isRtgWetColumn(World world, int x, int z) {
        ChunkLandscape landscape = sampleLandscape(world, x, z);
        if (landscape == null || landscape.noise == null) return false;
        int localX = x & 15;
        int localZ = z & 15;
        int index = localX * 16 + localZ;
        BiomeProvider provider = world.getBiomeProvider();
        return isWetColumn(provider, landscape, index, x, z);
    }

    private static Float sampleRtgHeight(World world, int x, int z) {
        ChunkLandscape landscape = sampleLandscape(world, x, z);
        if (landscape == null || landscape.noise == null) return null;
        int index = (x & 15) * 16 + (z & 15);
        if (index < 0 || index >= landscape.noise.length) return null;
        return landscape.noise[index];
    }

    private static ChunkLandscape sampleLandscape(World world, int x, int z) {
        if (world == null) return null;
        Object gen = Reflect.getChunkGenerator(world);
        if (!(gen instanceof ChunkGeneratorRTG)) return null;
        SAMPLING.set(SAMPLING.get() + 1);
        try {
            return ((ChunkGeneratorRTG) gen).getLandscape(world.getBiomeProvider(), new ChunkPos(x >> 4, z >> 4));
        } catch (Throwable ignored) {
            return null;
        } finally {
            SAMPLING.set(Math.max(0, SAMPLING.get() - 1));
        }
    }

    private static int minWellHeight() {
        return Math.max(1, ArcanaQuestTweaksConfig.RtgModuleConfig.surface.villageMinWellHeight);
    }
}
