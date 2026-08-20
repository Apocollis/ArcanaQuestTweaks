package com.apocollis.aqtweaks.rtg;

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
 * Village placement: wet-column tests, ocean-well veto, and land retry slots for buildings.
 */
public final class VillageLandHelper {

    public static final float STRONG_RIVER = 0.7F;
    public static final int SEA_LEVEL = 63;
    public static final int VILLAGE_LAYOUT_RADIUS = 8;

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

    public static boolean isWaterAt(Object villageStart, int x, int z) {
        BiomeProvider provider = Reflect.getVillageStartBiomeProvider(villageStart);
        if (isOceanOrRiverBiome(Reflect.getBiome(provider, x, z))) {
            return true;
        }
        World world = currentWorld();
        if (world == null) {
            world = Reflect.getVillageStartWorld(villageStart);
        }
        return isRtgLandscapeWet(world, x, z);
    }

    public static boolean villageStartAllowed(World world, int chunkX, int chunkZ) {
        return startRejectReason(world, chunkX, chunkZ) == null;
    }

    /**
     * Only reject a well that sits in ocean. Near-ocean, beach, and river wells are allowed.
     */
    public static String startRejectReason(World world, int chunkX, int chunkZ) {
        if (world == null) return null;
        int wellX = chunkX * 16 + 2;
        int wellZ = chunkZ * 16 + 2;
        Biome biome = Reflect.getBiome(world.getBiomeProvider(), wellX, wellZ);
        if (isOceanBiome(biome)) {
            String name = biome != null && biome.getRegistryName() != null
                    ? biome.getRegistryName().toString() : "unknown";
            return "ocean_well " + name;
        }
        return null;
    }

    public static boolean isWetColumn(BiomeProvider provider, ChunkLandscape landscape, int index, int worldX, int worldZ) {
        if (isOceanOrRiverBiome(Reflect.getBiome(provider, worldX, worldZ))) {
            return true;
        }
        return isLandscapeWet(landscape, index);
    }

    public static boolean isOceanBiome(Biome biome) {
        if (biome == null || isBeachBiome(biome)) return false;
        try {
            if (BiomeDictionary.hasType(biome, BiomeDictionary.Type.OCEAN)) {
                return true;
            }
            if (biome.getRegistryName() == null) return false;
            String name = biome.getRegistryName().toString().toLowerCase();
            return name.contains("ocean") || name.contains("kelp") || name.contains("coral");
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean isOceanOrRiverBiome(Biome biome) {
        if (isOceanBiome(biome)) return true;
        if (biome == null || isBeachBiome(biome)) return false;
        try {
            if (BiomeDictionary.hasType(biome, BiomeDictionary.Type.RIVER)) {
                return true;
            }
            if (biome.getRegistryName() == null) return false;
            String name = biome.getRegistryName().toString().toLowerCase();
            return name.contains("river") && !name.contains("dried");
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean isBeachBiome(Biome biome) {
        if (biome == null) return false;
        try {
            if (BiomeDictionary.hasType(biome, BiomeDictionary.Type.BEACH)) {
                return true;
            }
            if (biome.getRegistryName() == null) return false;
            return biome.getRegistryName().toString().toLowerCase().contains("beach");
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
        return isWaterAt(villageStart, box[1], box[3]);
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
        if (maxStep <= 0) return out;

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

    private static boolean isLandscapeWet(ChunkLandscape landscape, int index) {
        if (landscape == null) return false;
        if (landscape.river != null && index >= 0 && index < landscape.river.length
                && Math.abs(landscape.river[index]) > STRONG_RIVER) {
            return true;
        }
        return landscape.noise != null && index >= 0 && index < landscape.noise.length
                && landscape.noise[index] < SEA_LEVEL;
    }

    private static boolean isRtgLandscapeWet(World world, int x, int z) {
        ChunkLandscape landscape = sampleLandscape(world, x, z);
        if (landscape == null) return false;
        int index = (x & 15) * 16 + (z & 15);
        return isLandscapeWet(landscape, index);
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
}
