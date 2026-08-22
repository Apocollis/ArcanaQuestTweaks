package com.apocollis.aqtweaks.rtg;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.util.Reflect;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.structure.MapGenVillage;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureVillagePieces;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import rtg.world.gen.ChunkGeneratorRTG;
import rtg.world.gen.ChunkLandscape;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Village placement: wet-column tests, ocean-like well veto, coast buffer, and land retry slots for buildings.
 */
public final class VillageLandHelper {

    public static final float STRONG_RIVER = 0.4F;
    public static final int VILLAGE_LAYOUT_RADIUS = 8;
    public static final int BANK_BLEND = 8;
    public static final float PATH_WET_FRACTION = 0.5F;

    private static final ThreadLocal<Deque<World>> WORLDS = ThreadLocal.withInitial(ArrayDeque::new);
    private static final ThreadLocal<Deque<ChunkGeneratorRTG>> GENERATORS = ThreadLocal.withInitial(ArrayDeque::new);
    private static final ThreadLocal<Integer> SAMPLING = ThreadLocal.withInitial(() -> 0);
    private static final Map<World, MapGenVillage> STASHED_VILLAGE = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<World, ChunkGeneratorRTG> STASHED_RTG = Collections.synchronizedMap(new WeakHashMap<>());
    private static IBlockState loamyGrass;
    private static boolean loamyGrassLoaded;

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

    public static void stashGenerators(World world, MapGenVillage village, ChunkGeneratorRTG rtg) {
        if (world == null) return;
        if (village != null) STASHED_VILLAGE.put(world, village);
        if (rtg != null) STASHED_RTG.put(world, rtg);
    }

    public static MapGenVillage stashedVillage(World world) {
        return world == null ? null : STASHED_VILLAGE.get(world);
    }

    public static ChunkGeneratorRTG stashedRtg(World world) {
        return world == null ? null : STASHED_RTG.get(world);
    }

    public static boolean isSamplingLandscape() {
        return SAMPLING.get() > 0;
    }

    public static int minWellHeight() {
        int value = ArcanaQuestTweaksConfig.RtgModuleConfig.surface.villageMinWellHeight;
        return Math.max(1, Math.min(255, value));
    }

    public static float minWellHeightF() {
        return minWellHeight();
    }

    /**
     * BOP loamy grass ({@code biomesoplenty:grass} meta 2). Null if BOP is missing.
     * Used only to replace mud on the village plate.
     */
    public static IBlockState bopLoamyGrass() {
        if (!loamyGrassLoaded) {
            loamyGrassLoaded = true;
            loamyGrass = null;
            try {
                if (Loader.isModLoaded("biomesoplenty")) {
                    Block grass = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("biomesoplenty", "grass"));
                    if (grass != null) {
                        loamyGrass = grass.getStateFromMeta(2);
                    }
                }
            } catch (Throwable ignored) {}
        }
        return loamyGrass;
    }

    public static boolean isBopMud(IBlockState state) {
        if (state == null || state.getBlock() == null) return false;
        try {
            ResourceLocation name = state.getBlock().getRegistryName();
            return name != null && "biomesoplenty".equals(name.getNamespace()) && "mud".equals(name.getPath());
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Houses and RC skip/retry on watercourses, not swamp-like land. Those pieces stay and get a land pad.
     * Ocean and river are always wet, even if a pack also tags them swamp.
     */
    public static boolean isWaterAt(Object villageStart, int x, int z) {
        return isBuildingWet(villageStart, x, z);
    }

    public static boolean isBuildingWet(Object villageStart, int x, int z) {
        World world = currentWorld();
        if (world == null) {
            world = Reflect.getVillageStartWorld(villageStart);
        }
        return isNeverRaiseAt(world, villageStart, x, z);
    }

    public static boolean isFloodedAt(Object villageStart, int x, int z) {
        World world = currentWorld();
        if (world == null) {
            world = Reflect.getVillageStartWorld(villageStart);
        }
        if (isNeverRaiseAt(world, villageStart, x, z)) return true;
        return isRtgLandscapeLake(world, x, z);
    }

    public static boolean villageStartAllowed(World world, int chunkX, int chunkZ) {
        return startRejectReason(world, chunkX, chunkZ) == null;
    }

    /**
     * Veto only when the well column is never-raise (ocean/river biome or RTG river)
     * and there is no dry land within retry distance. Dry land below min well Y is kept and raised.
     * Land wells too close to ocean still fail the coast buffer. Nearby river does not cancel a dry well.
     */
    public static String startRejectReason(World world, int chunkX, int chunkZ) {
        if (world == null) return null;
        int wellX = chunkX * 16 + 2;
        int wellZ = chunkZ * 16 + 2;
        BiomeProvider provider = world.getBiomeProvider();
        Biome biome = Reflect.getBiome(provider, wellX, wellZ);
        if (isNeverRaiseAt(world, wellX, wellZ)) {
            if (findDryWell(world, wellX, wellZ) != null) {
                return null;
            }
            if (isOceanBiome(biome)) {
                return "ocean_well " + biomeId(biome);
            }
            return "river_well " + biomeId(biome);
        }
        String coast = coastOceanRejectReason(provider, wellX, wellZ);
        if (coast != null) return coast;
        return null;
    }

    public static int[] resolvedWellXZ(World world, int wellX, int wellZ) {
        if (world == null || !isNeverRaiseAt(world, wellX, wellZ)) {
            return new int[] {wellX, wellZ};
        }
        int[] dry = findDryWell(world, wellX, wellZ);
        return dry != null ? dry : new int[] {wellX, wellZ};
    }

    public static int[] resolvedWellForChunk(World world, int chunkX, int chunkZ) {
        return resolvedWellXZ(world, chunkX * 16 + 2, chunkZ * 16 + 2);
    }

    /**
     * First dry (not never-raise) column within {@code villageWaterRetryDistance}, Chebyshev rings.
     */
    public static int[] findDryWell(World world, int wellX, int wellZ) {
        if (world == null) return null;
        int max = Math.max(0, ArcanaQuestTweaksConfig.RtgModuleConfig.surface.villageWaterRetryDistance);
        if (max <= 0) return null;
        Map<Long, ChunkLandscape> cache = new HashMap<>();
        for (int r = 1; r <= max; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                    int x = wellX + dx;
                    int z = wellZ + dz;
                    if (!isNeverRaiseAt(world, x, z, cache)) {
                        return new int[] {x, z};
                    }
                }
            }
        }
        return null;
    }

    public static void offsetStructureStart(Object start, int dx, int dz) {
        if (start == null || (dx == 0 && dz == 0)) return;
        for (Object piece : Reflect.getStructureStartComponents(start)) {
            Reflect.offsetStructureComponent(piece, dx, 0, dz);
        }
        Reflect.updateStructureStartBoundingBox(start);
    }

    /**
     * Drop Starts that fail {@link #startRejectReason} from {@code structureMap} and the plate cache.
     * {@code /locate Village} reads the map, so a vetoed well must not remain there.
     * Walked river/ocean wells pass {@code startRejectReason} and are kept.
     */
    public static void forgetRejectedStarts(MapGenVillage gen, World world) {
        if (gen == null || world == null) return;
        if (!ArcanaQuestTweaksConfig.RtgModuleConfig.surface.rejectCoastalVillageStarts) return;
        List<Object> snapshot = new ArrayList<>();
        for (Object start : Reflect.getMapGenStructureStarts(gen)) {
            snapshot.add(start);
        }
        long seed = Reflect.getSeed(world);
        for (Object start : snapshot) {
            int cx = Reflect.getStructureStartChunkX(start);
            int cz = Reflect.getStructureStartChunkZ(start);
            if (cx == Integer.MIN_VALUE || cz == Integer.MIN_VALUE) continue;
            String reason = startRejectReason(world, cx, cz);
            if (reason == null) continue;
            Reflect.removeStructureStart(gen, cx, cz);
            VillagePlate.forget(world, start, cx, cz);
            if (VillageDebug.once("forget:" + seed + ":" + cx + "," + cz)) {
                VillageDebug.log("forget chunk=%d,%d well=%d,%d %s",
                        cx, cz, cx * 16 + 2, cz * 16 + 2, reason);
            }
        }
    }

    /**
     * Reject a land/beach well if ocean-like is closer than the coast buffer. 0 = well column only.
     * Nearby river does not cancel a dry well.
     */
    private static String coastOceanRejectReason(BiomeProvider provider, int wellX, int wellZ) {
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
            }
        }
        return null;
    }

    /**
     * Ocean and river columns are never raised or plated, even inside a house or road box.
     * Also true when RTG river strength is high even if the biome provider says plains.
     */
    public static boolean isNeverRaiseBiome(Biome biome) {
        return isOceanOrRiverBiome(biome);
    }

    public static boolean isNeverRaiseAt(World world, int x, int z) {
        return isNeverRaiseAt(world, x, z, null);
    }

    public static boolean isNeverRaiseAt(World world, Object villageStart, int x, int z) {
        if (world != null) {
            return isNeverRaiseAt(world, x, z, null);
        }
        BiomeProvider provider = Reflect.getVillageStartBiomeProvider(villageStart);
        return isNeverRaiseBiome(Reflect.getBiome(provider, x, z));
    }

    public static boolean isNeverRaiseColumn(Biome biome, ChunkLandscape landscape, int index) {
        return isNeverRaiseBiome(biome) || isLandscapeNeverRaise(landscape, index);
    }

    private static boolean isNeverRaiseAt(World world, int x, int z, Map<Long, ChunkLandscape> cache) {
        if (world == null) return false;
        Biome biome = Reflect.getBiome(world.getBiomeProvider(), x, z);
        if (isNeverRaiseBiome(biome)) return true;
        ChunkLandscape landscape = cache != null ? landscapeCached(world, x, z, cache) : sampleLandscape(world, x, z);
        int index = (x & 15) * 16 + (z & 15);
        return isLandscapeNeverRaise(landscape, index);
    }

    private static ChunkLandscape landscapeCached(World world, int x, int z, Map<Long, ChunkLandscape> cache) {
        long key = ChunkPos.asLong(x >> 4, z >> 4);
        if (cache.containsKey(key)) return cache.get(key);
        ChunkLandscape landscape = sampleLandscape(world, x, z);
        cache.put(key, landscape);
        return landscape;
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
        return isNeverRaiseColumn(biome, landscape, index);
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

    public static boolean isVillageWellOrStart(Object component) {
        return component instanceof StructureVillagePieces.Start
                || component instanceof StructureVillagePieces.Well;
    }

    /**
     * Drop a layout piece from the start lists so it cannot paste or grow more roads/houses.
     */
    public static void removeVillagePiece(Object start, List<?> pieces, Object placed) {
        if (placed == null) return;
        if (pieces != null) {
            pieces.remove(placed);
        }
        if (start == null) return;
        for (Class<?> type = start.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (!List.class.isAssignableFrom(field.getType())) continue;
                try {
                    field.setAccessible(true);
                    Object list = field.get(start);
                    if (list instanceof List) {
                        ((List<?>) list).remove(placed);
                    }
                } catch (Throwable ignored) {
                }
            }
        }
    }

    /**
     * Layout the current chunk plus nearby village wells.
     * Wells sit at a seeded offset inside each spacing cell, not at the cell origin.
     */
    public static void layoutVillageGrid(MapGenVillage gen, World world, int cx, int cz, ChunkPrimer primer) {
        if (gen == null || world == null) return;
        gen.generate(world, cx, cz, primer);
        int spacing = Reflect.getVillageDistance(gen);
        if (spacing < 9) spacing = 32;
        int minTown = Reflect.getVillageMinDistance(gen);
        if (minTown < 1 || minTown >= spacing) minTown = 8;
        int minCellX = villageCell(cx - VILLAGE_LAYOUT_RADIUS, spacing);
        int maxCellX = villageCell(cx + VILLAGE_LAYOUT_RADIUS, spacing);
        int minCellZ = villageCell(cz - VILLAGE_LAYOUT_RADIUS, spacing);
        int maxCellZ = villageCell(cz + VILLAGE_LAYOUT_RADIUS, spacing);
        if (minCellX > maxCellX) {
            int tmp = minCellX;
            minCellX = maxCellX;
            maxCellX = tmp;
        }
        if (minCellZ > maxCellZ) {
            int tmp = minCellZ;
            minCellZ = maxCellZ;
            maxCellZ = tmp;
        }
        long seed = Reflect.getSeed(world);
        stashGenerators(world, gen, currentGenerator());
        for (int cellX = minCellX; cellX <= maxCellX; cellX++) {
            for (int cellZ = minCellZ; cellZ <= maxCellZ; cellZ++) {
                int[] well = villageWellChunk(seed, cellX, cellZ, spacing, minTown);
                int gx = well[0];
                int gz = well[1];
                if (gx == cx && gz == cz) continue;
                gen.generate(world, gx, gz, primer);
                if (VillageDebug.once("layout:" + seed + ":" + cellX + "," + cellZ)) {
                    VillageDebug.log("layout cell=%d,%d origin=%d,%d wellChunk=%d,%d hit=%s",
                            cellX, cellZ, cellX * spacing, cellZ * spacing, gx, gz,
                            villageStartAt(world, gx, gz) ? "yes" : "no");
                }
            }
        }
        forgetRejectedStarts(gen, world);
    }

    /**
     * Vanilla {@code MapGenVillage} cell index. Matches toward-zero division after the
     * negative-chunk subtract, not {@code floorDiv}.
     */
    public static int villageCell(int chunk, int spacing) {
        int c = chunk;
        if (c < 0) {
            c -= spacing - 1;
        }
        return c / spacing;
    }

    /**
     * Well chunk inside a village cell. Same RNG as vanilla {@code canSpawnStructureAtCoords}
     * ({@code World.setRandomSeed(cellX, cellZ, 10387312)}) without mutating {@code world.rand}.
     */
    public static int[] villageWellChunk(long worldSeed, int cellX, int cellZ, int spacing, int minTown) {
        int span = Math.max(1, spacing - minTown);
        long rngSeed = (long) cellX * 341873128712L
                + (long) cellZ * 132897987541L
                + worldSeed
                + 10387312L;
        Random random = new Random(rngSeed);
        return new int[] {
                cellX * spacing + random.nextInt(span),
                cellZ * spacing + random.nextInt(span)
        };
    }

    private static boolean villageStartAt(World world, int chunkX, int chunkZ) {
        if (world == null) return false;
        int wellX = chunkX * 16 + 2;
        int wellZ = chunkZ * 16 + 2;
        for (VillagePlate.Record rec : VillagePlate.starts(Reflect.getSeed(world))) {
            if (rec.wellX == wellX && rec.wellZ == wellZ) return true;
            int cx = Reflect.getStructureStartChunkX(rec.start);
            int cz = Reflect.getStructureStartChunkZ(rec.start);
            if (cx == chunkX && cz == chunkZ) return true;
        }
        return false;
    }

    public static boolean isAabbWet(Object villageStart, Object component) {
        return isAabbWet(villageStart, component, false);
    }

    public static boolean isAabbFlooded(Object villageStart, Object component) {
        return isAabbWet(villageStart, component, true);
    }

    /**
     * True only if every column is flooded. Mixed road + puddle still plates with houses.
     * Fully flooded roads are omitted from layout (no wooden docks).
     */
    public static boolean isAabbFullyFlooded(Object villageStart, Object component) {
        return wetFraction(villageStart, component, true) >= 1.0F;
    }

    /**
     * True if at least half the path columns are wet. Keeps a forest path with a puddle;
     * drops a plank bridge over a lake.
     */
    public static boolean isAabbMostlyWet(Object villageStart, Object component) {
        return isAabbFullyFlooded(villageStart, component)
                || wetFraction(villageStart, component, false) >= PATH_WET_FRACTION;
    }

    /**
     * True if any column is ocean-like, river biome, or RTG river. Those columns never get village pieces or a plate.
     */
    public static boolean isAabbTouchesOceanOrRiver(Object villageStart, Object component) {
        int[] box = Reflect.getStructureComponentBoxXZ(component);
        if (box == null) return false;
        World world = currentWorld();
        if (world == null) {
            world = Reflect.getVillageStartWorld(villageStart);
        }
        BiomeProvider provider = Reflect.getVillageStartBiomeProvider(villageStart);
        for (int x = box[0]; x <= box[1]; x++) {
            for (int z = box[2]; z <= box[3]; z++) {
                if (isNeverRaiseAt(world, villageStart, x, z)) {
                    return true;
                }
                if (world == null && isNeverRaiseBiome(Reflect.getBiome(provider, x, z))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Drop a path that crosses ocean/river, or that is mostly lake. Swamp paths stay.
     */
    public static boolean shouldOmitPath(Object villageStart, Object component) {
        return isAabbTouchesOceanOrRiver(villageStart, component)
                || isAabbMostlyWet(villageStart, component);
    }

    public static String pathOmitReason(Object villageStart, Object component) {
        if (isAabbTouchesOceanOrRiver(villageStart, component)) {
            int[] box = Reflect.getStructureComponentBoxXZ(component);
            BiomeProvider provider = Reflect.getVillageStartBiomeProvider(villageStart);
            Biome biome = box == null ? null : Reflect.getBiome(provider, box[0], box[2]);
            return "ocean_or_river " + biomeId(biome);
        }
        return String.format("mostly_wet %.2f", wetFraction(villageStart, component, false));
    }

    private static boolean isAabbWet(Object villageStart, Object component, boolean flooded) {
        return wetFraction(villageStart, component, flooded) > 0.0F;
    }

    private static float wetFraction(Object villageStart, Object component, boolean flooded) {
        int[] box = Reflect.getStructureComponentBoxXZ(component);
        if (box == null) return 0.0F;
        int wet = 0;
        int total = 0;
        for (int x = box[0]; x <= box[1]; x++) {
            for (int z = box[2]; z <= box[3]; z++) {
                total++;
                if (flooded ? isFloodedAt(villageStart, x, z) : isBuildingWet(villageStart, x, z)) {
                    wet++;
                }
            }
        }
        return total == 0 ? 0.0F : (float) wet / (float) total;
    }

    /**
     * Last-resort populate check: do not paste a village building onto ocean/river or open liquid.
     * Roads, the well, and swamp-like liquids stay. Uses surface height, not {@code getTopSolidOrLiquidBlock}
     * (1.12 that method skips water and hits the seafloor).
     */
    public static boolean isOceanOrRiverFloor(World world, Object component, StructureBoundingBox clip) {
        if (world == null || world.isRemote || component == null) return false;
        if (!(component instanceof StructureVillagePieces.Village)) return false;
        if (isVillageRoad(component) || isVillageWellOrStart(component)) return false;
        int[] box = Reflect.getStructureComponentBoxXZ(component);
        if (box == null) return false;
        int minX = box[0];
        int maxX = box[1];
        int minZ = box[2];
        int maxZ = box[3];
        if (clip != null) {
            minX = Math.max(minX, clip.minX);
            maxX = Math.min(maxX, clip.maxX);
            minZ = Math.max(minZ, clip.minZ);
            maxZ = Math.min(maxZ, clip.maxZ);
        }
        if (minX > maxX || minZ > maxZ) return false;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                int y = Math.max(1, world.getHeight(x, z) - 1);
                BlockPos pos = new BlockPos(x, y, z);
                Biome biome = world.getBiome(pos);
                if (isNeverRaiseBiome(biome)) return true;
                if (isSwampLikeForRaise(biome)) continue;
                IBlockState state = world.getBlockState(pos);
                if (state != null && state.getMaterial().isLiquid()) return true;
            }
        }
        return false;
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

    public static boolean isLandscapeNeverRaise(ChunkLandscape landscape, int index) {
        return landscape != null && landscape.river != null && index >= 0 && index < landscape.river.length
                && Math.abs(landscape.river[index]) > STRONG_RIVER;
    }

    public static boolean isLandscapeLake(ChunkLandscape landscape, int index) {
        if (isLandscapeNeverRaise(landscape, index)) return false;
        return landscape != null && landscape.noise != null && index >= 0 && index < landscape.noise.length
                && landscape.noise[index] < minWellHeightF();
    }

    public static boolean isLandscapeWet(ChunkLandscape landscape, int index) {
        return isLandscapeNeverRaise(landscape, index) || isLandscapeLake(landscape, index);
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

    private static boolean isRtgLandscapeLake(World world, int x, int z) {
        if (isSamplingLandscape()) {
            return false;
        }
        ChunkLandscape landscape = sampleLandscape(world, x, z);
        if (landscape == null) {
            return false;
        }
        int index = (x & 15) * 16 + (z & 15);
        return isLandscapeLake(landscape, index);
    }

    private static ChunkLandscape sampleLandscape(World world, int x, int z) {
        ChunkGeneratorRTG gen = currentGenerator();
        if (gen == null) {
            gen = StructureVillageOverlap.findRtgGenerator(world);
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
