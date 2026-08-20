package com.apocollis.aqtweaks.rtg;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.util.Reflect;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.structure.MapGenVillage;
import net.minecraftforge.common.BiomeDictionary;
import rtg.world.gen.ChunkGeneratorRTG;
import rtg.world.gen.ChunkLandscape;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Village placement: wet-column tests, ocean-like well veto, coast buffer, and land retry slots for buildings.
 */
public final class VillageLandHelper {

    public static final float STRONG_RIVER = 0.4F;
    public static final int SEA_LEVEL = 63;
    public static final int FLOOD_LEVEL = 64;
    public static final int VILLAGE_LAYOUT_RADIUS = 8;
    public static final int BANK_BLEND = 8;

    private static final ThreadLocal<Deque<World>> WORLDS = ThreadLocal.withInitial(ArrayDeque::new);
    private static final ThreadLocal<Deque<ChunkGeneratorRTG>> GENERATORS = ThreadLocal.withInitial(ArrayDeque::new);
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

    public static void pushGenerator(ChunkGeneratorRTG gen) {
        if (gen != null) GENERATORS.get().push(gen);
    }

    public static void popGenerator() {
        Deque<ChunkGeneratorRTG> stack = GENERATORS.get();
        if (!stack.isEmpty()) {
            stack.pop();
        }
    }

    public static ChunkGeneratorRTG currentGenerator() {
        Deque<ChunkGeneratorRTG> stack = GENERATORS.get();
        return stack.isEmpty() ? null : stack.peek();
    }

    public static boolean isSamplingLandscape() {
        return SAMPLING.get() > 0;
    }

    /**
     * Houses and RC skip/retry on watercourses, not swamp-like land. Those pieces stay and get a land pad.
     * Ocean and river are always wet, even if a pack also tags them swamp.
     */
    public static boolean isWaterAt(Object villageStart, int x, int z) {
        return isBuildingWet(villageStart, x, z);
    }

    public static boolean isBuildingWet(Object villageStart, int x, int z) {
        BiomeProvider provider = Reflect.getVillageStartBiomeProvider(villageStart);
        Biome biome = Reflect.getBiome(provider, x, z);
        if (isNeverRaiseBiome(biome)) return true;
        if (isSwampLikeForRaise(biome)) return false;
        return isFloodedAt(villageStart, x, z);
    }

    public static boolean isFloodedAt(Object villageStart, int x, int z) {
        BiomeProvider provider = Reflect.getVillageStartBiomeProvider(villageStart);
        Biome biome = Reflect.getBiome(provider, x, z);
        if (isOceanOrRiverBiome(biome)) return true;
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
     * Veto ocean-like, river, or a flooded watercourse. Swamp wells are allowed.
     * Beach wells are allowed unless ocean-like or river is closer than {@code villageCoastBuffer}.
     */
    public static String startRejectReason(World world, int chunkX, int chunkZ) {
        if (world == null) return null;
        int wellX = chunkX * 16 + 2;
        int wellZ = chunkZ * 16 + 2;
        BiomeProvider provider = world.getBiomeProvider();
        Biome biome = Reflect.getBiome(provider, wellX, wellZ);
        if (isOceanBiome(biome)) {
            return "ocean_well " + biomeId(biome);
        }
        if (isOceanOrRiverBiome(biome)) {
            return "river_well " + biomeId(biome);
        }
        String coast = coastRejectReason(provider, wellX, wellZ);
        if (coast != null) return coast;
        if (isSwampLikeForRaise(biome)) return null;
        if (isRtgLandscapeWet(world, wellX, wellZ)) {
            return "flooded_well " + biomeId(biome);
        }
        return null;
    }

    /**
     * Reject a land/beach well if ocean-like or river is closer than the coast buffer. 0 = well column only.
     */
    private static String coastRejectReason(BiomeProvider provider, int wellX, int wellZ) {
        int buffer = Math.max(0, ArcanaQuestTweaksConfig.RtgModuleConfig.surface.villageCoastBuffer);
        if (buffer <= 0 || provider == null) return null;
        int limit = buffer - 1;
        for (int dx = -limit; dx <= limit; dx += 2) {
            for (int dz = -limit; dz <= limit; dz += 2) {
                if (dx == 0 && dz == 0) continue;
                if (Math.max(Math.abs(dx), Math.abs(dz)) >= buffer) continue;
                Biome nearby = Reflect.getBiome(provider, wellX + dx, wellZ + dz);
                if (isOceanBiome(nearby)) {
                    return "coast_ocean " + biomeId(nearby);
                }
                if (isOceanOrRiverBiome(nearby)) {
                    return "coast_river " + biomeId(nearby);
                }
            }
        }
        return null;
    }

    /**
     * Ocean and river columns are never raised or plated, even inside a house or road box.
     */
    public static boolean isNeverRaiseBiome(Biome biome) {
        return isOceanOrRiverBiome(biome);
    }

    /**
     * Swamp-like land that may be raised under houses. Never ocean or river.
     */
    public static boolean isSwampLikeForRaise(Biome biome) {
        if (isNeverRaiseBiome(biome)) return false;
        return isSwampBiome(biome);
    }

    /**
     * True for ocean/river biome, or RTG water that is not swamp-like (those get a rounded pad instead).
     */
    public static boolean isFlattenSkipColumn(BiomeProvider provider, ChunkLandscape landscape, int index, int worldX, int worldZ) {
        Biome biome = Reflect.getBiome(provider, worldX, worldZ);
        if (isNeverRaiseBiome(biome)) {
            return true;
        }
        return isLandscapeWet(landscape, index);
    }

    public static boolean isWetColumn(BiomeProvider provider, ChunkLandscape landscape, int index, int worldX, int worldZ) {
        return isFlattenSkipColumn(provider, landscape, index, worldX, worldZ);
    }

    /**
     * Ocean, kelp forest, coral reef, and similar water biomes. Pure beach is not ocean.
     */
    public static boolean isOceanBiome(Biome biome) {
        if (biome == null) return false;
        if (oceanLikeName(biome)) return true;
        if (isBeachBiome(biome)) return false;
        try {
            if (BiomeDictionary.hasType(biome, BiomeDictionary.Type.OCEAN)) {
                return true;
            }
            if (swampTagged(biome)) return false;
            return BiomeDictionary.hasType(biome, BiomeDictionary.Type.WATER);
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

    public static boolean isSwampBiome(Biome biome) {
        if (biome == null || isBeachBiome(biome) || isNeverRaiseBiome(biome)) return false;
        return swampTagged(biome);
    }

    private static boolean swampTagged(Biome biome) {
        if (biome == null) return false;
        try {
            if (BiomeDictionary.hasType(biome, BiomeDictionary.Type.SWAMP)) {
                return true;
            }
            if (biome.getRegistryName() == null) return false;
            String name = biome.getRegistryName().toString().toLowerCase();
            return name.contains("swamp") || name.contains("marsh") || name.contains("bog")
                    || name.contains("wetland") || name.contains("bayou")
                    || name.contains("mangrove") || name.contains("fen")
                    || name.contains("moor") || name.contains("peat") || name.contains("muskeg");
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean oceanLikeName(Biome biome) {
        try {
            if (biome == null || biome.getRegistryName() == null) return false;
            String name = biome.getRegistryName().toString().toLowerCase();
            return name.contains("ocean") || name.contains("kelp") || name.contains("coral")
                    || name.contains("reef") || name.contains("atoll") || name.contains("lagoon");
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static String biomeId(Biome biome) {
        if (biome == null || biome.getRegistryName() == null) return "unknown";
        return biome.getRegistryName().toString();
    }

    public static boolean isVillageRoad(Object component) {
        if (component == null) return false;
        String name = component.getClass().getName();
        return name.contains("StructureVillagePieces$Path")
                || name.contains("StructureVillagePieces$Road")
                || name.contains("StructureVillagePieces.Path")
                || name.contains("StructureVillagePieces.Road");
    }

    /**
     * Layout the current chunk plus village-grid cell origins in range.
     * Does not call generate() on all 289 neighbors.
     */
    public static void layoutVillageGrid(MapGenVillage gen, World world, int cx, int cz, ChunkPrimer primer) {
        if (gen == null || world == null) return;
        gen.generate(world, cx, cz, primer);
        int spacing = Reflect.getVillageDistance(gen);
        if (spacing < 9) spacing = 32;
        int minCellX = Math.floorDiv(cx - VILLAGE_LAYOUT_RADIUS, spacing);
        int maxCellX = Math.floorDiv(cx + VILLAGE_LAYOUT_RADIUS, spacing);
        int minCellZ = Math.floorDiv(cz - VILLAGE_LAYOUT_RADIUS, spacing);
        int maxCellZ = Math.floorDiv(cz + VILLAGE_LAYOUT_RADIUS, spacing);
        for (int cellX = minCellX; cellX <= maxCellX; cellX++) {
            for (int cellZ = minCellZ; cellZ <= maxCellZ; cellZ++) {
                int gx = cellX * spacing;
                int gz = cellZ * spacing;
                if (gx == cx && gz == cz) continue;
                gen.generate(world, gx, gz, primer);
            }
        }
    }

    public static boolean isAabbWet(Object villageStart, Object component) {
        return isAabbWet(villageStart, component, false);
    }

    public static boolean isAabbFlooded(Object villageStart, Object component) {
        return isAabbWet(villageStart, component, true);
    }

    /**
     * True only if every sampled column is flooded. Mixed road + puddle still plates with houses.
     * Fully flooded roads (docks) stay off the plate hull.
     */
    public static boolean isAabbFullyFlooded(Object villageStart, Object component) {
        int[] box = Reflect.getStructureComponentBoxXZ(component);
        if (box == null) return false;
        int stride = 2;
        for (int x = box[0]; x <= box[1]; x += stride) {
            for (int z = box[2]; z <= box[3]; z += stride) {
                if (!isFloodedAt(villageStart, x, z)) {
                    return false;
                }
            }
        }
        return isFloodedAt(villageStart, box[1], box[3]);
    }

    private static boolean isAabbWet(Object villageStart, Object component, boolean flooded) {
        int[] box = Reflect.getStructureComponentBoxXZ(component);
        if (box == null) return false;
        int stride = 2;
        for (int x = box[0]; x <= box[1]; x += stride) {
            for (int z = box[2]; z <= box[3]; z += stride) {
                if (flooded ? isFloodedAt(villageStart, x, z) : isBuildingWet(villageStart, x, z)) {
                    return true;
                }
            }
        }
        return flooded
                ? isFloodedAt(villageStart, box[1], box[3])
                : isBuildingWet(villageStart, box[1], box[3]);
    }

    public static boolean withinVillageCap(Object villageStart, int x, int z) {
        int[] box = Reflect.getStructureComponentBoxXZ(villageStart);
        if (box == null) return true;
        return Math.abs(x - box[0]) <= 112 && Math.abs(z - box[2]) <= 112;
    }

    public static boolean isWaystonePiece(Object component) {
        if (component == null) return false;
        return component.getClass().getName().contains("ComponentVillageWaystone");
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

    /**
     * Street slots, then toward the well, then a spiral around the well. Dry columns only, inside the village cap.
     */
    public static List<int[]> inlandCandidates(Object villageStart, int x, int z, EnumFacing facing, int streetMax) {
        List<int[]> out = new ArrayList<>();
        LinkedHashSet<Long> seen = new LinkedHashSet<>();
        for (int[] slot : landCandidates(villageStart, x, z, facing, streetMax)) {
            addLandSlot(out, seen, villageStart, slot[0], slot[1]);
        }
        int wellX = x;
        int wellZ = z;
        int[] startBox = Reflect.getStructureComponentBoxXZ(villageStart);
        if (startBox != null) {
            wellX = (startBox[0] + startBox[1]) >> 1;
            wellZ = (startBox[2] + startBox[3]) >> 1;
        }
        addLandSlot(out, seen, villageStart, wellX, wellZ);
        int sx = Integer.signum(wellX - x);
        int sz = Integer.signum(wellZ - z);
        int dist = Math.max(Math.abs(wellX - x), Math.abs(wellZ - z));
        for (int step = 4; step <= Math.max(dist, 4); step += 4) {
            int nx = sx == 0 ? x : x + sx * step;
            int nz = sz == 0 ? z : z + sz * step;
            if (sx != 0 && Math.abs(nx - x) >= Math.abs(wellX - x)) nx = wellX;
            if (sz != 0 && Math.abs(nz - z) >= Math.abs(wellZ - z)) nz = wellZ;
            addLandSlot(out, seen, villageStart, nx, nz);
            if (nx == wellX && nz == wellZ) break;
        }
        for (int r = 4; r <= 80; r += 4) {
            for (int ox = -r; ox <= r; ox += 4) {
                addLandSlot(out, seen, villageStart, wellX + ox, wellZ - r);
                addLandSlot(out, seen, villageStart, wellX + ox, wellZ + r);
            }
            for (int oz = -r + 4; oz <= r - 4; oz += 4) {
                addLandSlot(out, seen, villageStart, wellX - r, wellZ + oz);
                addLandSlot(out, seen, villageStart, wellX + r, wellZ + oz);
            }
        }
        return out;
    }

    private static void addLandSlot(List<int[]> out, Set<Long> seen, Object villageStart, int x, int z) {
        if (!withinVillageCap(villageStart, x, z)) return;
        if (isWaterAt(villageStart, x, z)) return;
        long key = ((long) x << 32) | (z & 0xFFFFFFFFL);
        if (!seen.add(key)) return;
        out.add(new int[] {x, z});
    }

    public static boolean isLandscapeWet(ChunkLandscape landscape, int index) {
        if (landscape == null) return false;
        if (landscape.river != null && index >= 0 && index < landscape.river.length
                && Math.abs(landscape.river[index]) > STRONG_RIVER) {
            return true;
        }
        return landscape.noise != null && index >= 0 && index < landscape.noise.length
                && landscape.noise[index] < FLOOD_LEVEL;
    }

    public static float sampleNoise(World world, int x, int z) {
        ChunkLandscape landscape = sampleLandscape(world, x, z);
        return heightAt(landscape, x, z);
    }

    /**
     * Sample RTG noise from a live generator. Used while ChunkGeneratorRTG is already on the stack,
     * so we do not go through World#getChunkProvider wrapping.
     */
    public static float sampleNoise(ChunkGeneratorRTG gen, BiomeProvider provider, int x, int z) {
        if (gen == null || provider == null) return Float.NaN;
        SAMPLING.set(SAMPLING.get() + 1);
        try {
            return heightAt(gen.getLandscape(provider, new ChunkPos(x >> 4, z >> 4)), x, z);
        } catch (Throwable ignored) {
            return Float.NaN;
        } finally {
            SAMPLING.set(Math.max(0, SAMPLING.get() - 1));
        }
    }

    public static boolean isUsableHeight(float height) {
        return !Float.isNaN(height) && height > 1.0F;
    }

    private static float heightAt(ChunkLandscape landscape, int x, int z) {
        if (landscape == null || landscape.noise == null) return Float.NaN;
        int index = (x & 15) * 16 + (z & 15);
        if (index < 0 || index >= landscape.noise.length) return Float.NaN;
        return landscape.noise[index];
    }

    private static boolean isRtgLandscapeWet(World world, int x, int z) {
        ChunkLandscape landscape = sampleLandscape(world, x, z);
        if (landscape == null) return false;
        int index = (x & 15) * 16 + (z & 15);
        return isLandscapeWet(landscape, index);
    }

    private static ChunkLandscape sampleLandscape(World world, int x, int z) {
        ChunkGeneratorRTG gen = currentGenerator();
        if (gen == null) {
            Object found = Reflect.getChunkGenerator(world);
            if (found instanceof ChunkGeneratorRTG) {
                gen = (ChunkGeneratorRTG) found;
            }
        }
        if (gen == null) return null;
        SAMPLING.set(SAMPLING.get() + 1);
        try {
            BiomeProvider provider = world != null ? world.getBiomeProvider() : null;
            if (provider == null) return null;
            return gen.getLandscape(provider, new ChunkPos(x >> 4, z >> 4));
        } catch (Throwable ignored) {
            return null;
        } finally {
            SAMPLING.set(Math.max(0, SAMPLING.get() - 1));
        }
    }
}
