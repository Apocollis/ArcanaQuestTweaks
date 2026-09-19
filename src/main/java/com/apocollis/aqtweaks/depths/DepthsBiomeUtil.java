package com.apocollis.aqtweaks.depths;

import com.apocollis.aqtweaks.util.Reflect;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Shared biome checks for depths seam / water flood behavior.
 */
public final class DepthsBiomeUtil {

    private static final int MASK_CACHE_MAX = 64;

    private static final ThreadLocal<BlockPos.MutableBlockPos> POS =
            ThreadLocal.withInitial(BlockPos.MutableBlockPos::new);

    private static final ThreadLocal<Map<MaskKey, boolean[]>> MASKS =
            ThreadLocal.withInitial(() -> lru(MASK_CACHE_MAX));

    private DepthsBiomeUtil() {}

    public static boolean isWaterBiome(World world, int x, int z) {
        if (world == null) return false;
        int dim = PrimerAccess.dimensionOf(world);
        if (dim == Integer.MIN_VALUE) return lookupColumn(world, x, z);

        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        long seed = ChunkAccess.getSeed(world);
        MaskKey key = new MaskKey(seed, dim, chunkX, chunkZ);
        Map<MaskKey, boolean[]> cache = MASKS.get();
        boolean[] mask = cache.get(key);
        if (mask == null) {
            mask = new boolean[256];
            int startX = chunkX * 16;
            int startZ = chunkZ * 16;
            for (int lx = 0; lx < 16; ++lx) {
                for (int lz = 0; lz < 16; ++lz) {
                    mask[lz * 16 + lx] = lookupColumn(world, startX + lx, startZ + lz);
                }
            }
            cache.put(key, mask);
        }
        return mask[(z & 15) * 16 + (x & 15)];
    }

    private static boolean lookupColumn(World world, int x, int z) {
        try {
            Biome biome = null;
            Biome fallback = Reflect.getPlainsBiome();
            if (world.getBiomeProvider() != null) {
                biome = world.getBiomeProvider().getBiome(POS.get().setPos(x, 64, z), fallback);
            }
            return isWaterBiome(biome);
        } catch (Throwable ignored) {
        }
        return false;
    }

    public static boolean isWaterBiome(Biome biome) {
        if (biome == null) return false;
        try {
            if (BiomeDictionary.hasType(biome, BiomeDictionary.Type.WATER)
                    || BiomeDictionary.hasType(biome, BiomeDictionary.Type.OCEAN)
                    || BiomeDictionary.hasType(biome, BiomeDictionary.Type.RIVER)
                    || BiomeDictionary.hasType(biome, BiomeDictionary.Type.BEACH)) {
                return true;
            }
            if (biome.getRegistryName() != null) {
                String name = biome.getRegistryName().toString().toLowerCase();
                return name.contains("ocean") || name.contains("deep_ocean")
                        || name.contains("beach") || name.contains("river")
                        || name.contains("coral") || name.contains("kelp");
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static <V> Map<MaskKey, V> lru(final int max) {
        return new LinkedHashMap<MaskKey, V>(32, 0.75f, false) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<MaskKey, V> eldest) {
                return size() > max;
            }
        };
    }

    private static final class MaskKey {
        private final long seed;
        private final int dim;
        private final int chunkX;
        private final int chunkZ;

        private MaskKey(long seed, int dim, int chunkX, int chunkZ) {
            this.seed = seed;
            this.dim = dim;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof MaskKey)) return false;
            MaskKey other = (MaskKey) o;
            return seed == other.seed && dim == other.dim && chunkX == other.chunkX && chunkZ == other.chunkZ;
        }

        @Override
        public int hashCode() {
            int h = Long.hashCode(seed);
            h = 31 * h + dim;
            h = 31 * h + chunkX;
            h = 31 * h + chunkZ;
            return h;
        }
    }
}
