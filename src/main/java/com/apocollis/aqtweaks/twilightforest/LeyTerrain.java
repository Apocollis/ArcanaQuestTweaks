package com.apocollis.aqtweaks.twilightforest;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.mixin.vanilla.AccessorChunkProviderServer;
import com.apocollis.aqtweaks.nether.LoadedBlocks;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import twilightforest.TFFeature;
import twilightforest.biomes.TFBiomes;
import twilightforest.world.ChunkGeneratorTFBase;

import java.util.HashMap;
import java.util.Map;

/**
 * Twilight Forest terrain reads for the Ley-Rupture: the surface-relative exposure factor ({@link GroundReference}),
 * the biome name and intensity, and the structure-bounds shelter. All reads use loaded chunks only
 * ({@link LoadedBlocks}); the structure lookup reads the generator's in-memory structure starts.
 */
public final class LeyTerrain {
    private LeyTerrain() {}

    private static final Map<Biome, String> BIOME_NAMES = new HashMap<>();

    static {
        BIOME_NAMES.put(TFBiomes.tfLake, "tfLake");
        BIOME_NAMES.put(TFBiomes.twilightForest, "twilightForest");
        BIOME_NAMES.put(TFBiomes.denseTwilightForest, "denseTwilightForest");
        BIOME_NAMES.put(TFBiomes.highlands, "highlands");
        BIOME_NAMES.put(TFBiomes.mushrooms, "mushrooms");
        BIOME_NAMES.put(TFBiomes.tfSwamp, "tfSwamp");
        BIOME_NAMES.put(TFBiomes.stream, "stream");
        BIOME_NAMES.put(TFBiomes.snowy_forest, "snowy_forest");
        BIOME_NAMES.put(TFBiomes.glacier, "glacier");
        BIOME_NAMES.put(TFBiomes.clearing, "clearing");
        BIOME_NAMES.put(TFBiomes.oakSavanna, "oakSavanna");
        BIOME_NAMES.put(TFBiomes.fireflyForest, "fireflyForest");
        BIOME_NAMES.put(TFBiomes.deepMushrooms, "deepMushrooms");
        BIOME_NAMES.put(TFBiomes.darkForest, "darkForest");
        BIOME_NAMES.put(TFBiomes.enchantedForest, "enchantedForest");
        BIOME_NAMES.put(TFBiomes.fireSwamp, "fireSwamp");
        BIOME_NAMES.put(TFBiomes.darkForestCenter, "darkForestCenter");
        BIOME_NAMES.put(TFBiomes.highlandsCenter, "highlandsCenter");
        BIOME_NAMES.put(TFBiomes.thornlands, "thornlands");
        BIOME_NAMES.put(TFBiomes.spookyForest, "spookyForest");
    }

    private static String intensityKey;
    private static Map<String, Double> intensityTable = new HashMap<>();
    private static String extrasKey;
    private static Map<String, BiomeIntensity.Extra> extrasTable = new HashMap<>();

    public static String biomeName(World world, double x, double z) {
        Biome biome = world.getBiome(new BlockPos(x, 64, z));
        String name = BIOME_NAMES.get(biome);
        return name == null ? "twilightForest" : name;
    }

    public static double intensity(String biome) {
        String[] entries = ArcanaQuestTweaksConfig.TwilightForestModuleConfig.ley.biomeIntensity;
        String key = String.join("|", entries);
        if (!key.equals(intensityKey)) {
            intensityKey = key;
            intensityTable = BiomeIntensity.parseIntensity(entries);
        }
        return BiomeIntensity.intensityOf(intensityTable, biome,
                ArcanaQuestTweaksConfig.TwilightForestModuleConfig.ley.maxBiomeIntensity);
    }

    public static BiomeIntensity.Extra extra(String biome) {
        String[] entries = ArcanaQuestTweaksConfig.TwilightForestModuleConfig.ley.biomeExtras;
        String key = String.join("|", entries);
        if (!key.equals(extrasKey)) {
            extrasKey = key;
            extrasTable = BiomeIntensity.parseExtras(entries);
        }
        return extrasTable.get(biome);
    }

    private static boolean isGround(IBlockState state) {
        Material m = state.getMaterial();
        return m == Material.GROUND || m == Material.ROCK || m == Material.SAND || m == Material.CLAY
                || m == Material.GRASS || m == Material.ICE || m == Material.PACKED_ICE || m == Material.IRON
                || m == Material.WATER;
    }

    /** Surface Y of the column, scanning down from {@code startY} through leaves, logs, vines, plants and air. */
    private static Integer surfaceAt(World world, int x, int z, int startY) {
        if (LoadedBlocks.peek(world, x, Math.max(0, Math.min(255, startY)), z) == null) return null;
        return GroundReference.surfaceY(Math.min(255, startY), 0, y -> {
            IBlockState state = LoadedBlocks.peek(world, x, y, z);
            return state != null && isGround(state);
        });
    }

    /** Reference point for a position: the average of five column surfaces minus the offset. */
    public static double reference(World world, double px, double py, double pz) {
        int x = (int) Math.floor(px);
        int z = (int) Math.floor(pz);
        int start = (int) Math.floor(py) + 48;
        int[][] offsets = {{0, 0}, {8, 0}, {-8, 0}, {0, 8}, {0, -8}};
        int[] ys = new int[offsets.length];
        int n = 0;
        for (int[] o : offsets) {
            Integer y = surfaceAt(world, x + o[0], z + o[1], start);
            if (y != null) ys[n++] = y;
        }
        if (n == 0) return py;
        int[] used = new int[n];
        System.arraycopy(ys, 0, used, 0, n);
        return GroundReference.reference(used, ArcanaQuestTweaksConfig.TwilightForestModuleConfig.ley.referenceOffset);
    }

    /** 0..1 height exposure at a position (no shelter, no biome). */
    public static double heightExposure(World world, double px, double py, double pz) {
        return GroundReference.exposure(py, reference(world, px, py, pz),
                ArcanaQuestTweaksConfig.TwilightForestModuleConfig.ley.fadeHeight);
    }

    private static boolean isShelterFeature(TFFeature feature) {
        String name = feature.name();
        for (String allowed : ArcanaQuestTweaksConfig.TwilightForestModuleConfig.ley.shelterFeatures) {
            if (allowed.trim().equalsIgnoreCase(name)) return true;
        }
        return false;
    }

    /**
     * The full bounds (minX, minY, minZ, maxX, maxY, maxZ) of a Shelter Features structure whose footprint lies within
     * 8 blocks of the column, or null. Reads the generator's in-memory structure starts only (no chunk loading); the
     * caller samples on a coarse grid and caches, since this only drives where particles stop.
     */
    public static int[] shelterBoxNear(World world, int x, int z) {
        IChunkProvider provider = world.getChunkProvider();
        if (!(provider instanceof AccessorChunkProviderServer accessor)) return null;
        IChunkGenerator generator = accessor.aqtweaks$getChunkGenerator();
        if (!(generator instanceof ChunkGeneratorTFBase tf)) return null;
        try {
            StructureBoundingBox box = tf.getFullSBBNear(x, z, 8);
            if (box == null) return null;
            BlockPos centre = new BlockPos((box.minX + box.maxX) / 2, box.minY, (box.minZ + box.maxZ) / 2);
            TFFeature feature = tf.getFeatureAt(centre);
            if (feature == null || !isShelterFeature(feature)) return null;
            return new int[] {box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ};
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    /** True if the position is inside the bounds of a feature on the Shelter Features list. */
    public static boolean structureShelter(World world, BlockPos pos) {
        IChunkProvider provider = world.getChunkProvider();
        if (!(provider instanceof AccessorChunkProviderServer accessor)) return false;
        IChunkGenerator generator = accessor.aqtweaks$getChunkGenerator();
        if (!(generator instanceof ChunkGeneratorTFBase tf)) return false;
        try {
            if (!tf.isBlockInStructureBB(pos)) return false;
            TFFeature feature = tf.getFeatureAt(pos);
            if (feature == null) return false;
            String name = feature.name();
            for (String allowed : ArcanaQuestTweaksConfig.TwilightForestModuleConfig.ley.shelterFeatures) {
                if (allowed.trim().equalsIgnoreCase(name)) return true;
            }
        } catch (RuntimeException ignored) {
        }
        return false;
    }

    /** True if a real roof (an opaque non-leaf block) lies within 3 blocks above the position. */
    public static boolean roofed(World world, BlockPos pos) {
        for (int dy = 1; dy <= 3; dy++) {
            IBlockState state = LoadedBlocks.peek(world, pos.getX(), pos.getY() + dy, pos.getZ());
            if (state == null) return false;
            if (state.getLightOpacity(world, pos.up(dy)) > 0 && state.getMaterial() != Material.LEAVES
                    && !state.getBlock().isLeaves(state, world, pos.up(dy))) {
                return true;
            }
        }
        return false;
    }
}
