package com.apocollis.aqtweaks.rtg;

import com.apocollis.aqtweaks.mixin.vanilla.AccessorChunkProviderServer;
import com.apocollis.aqtweaks.mixin.vanilla.AccessorMapGenBase;
import com.apocollis.aqtweaks.mixin.vanilla.AccessorMapGenStructure;
import com.apocollis.aqtweaks.mixin.vanilla.AccessorMapGenVillage;
import com.apocollis.aqtweaks.mixin.vanilla.AccessorStructureComponent;
import com.apocollis.aqtweaks.mixin.vanilla.AccessorVillageStart;
import com.apocollis.aqtweaks.mixin.vanilla.InvokerStructureStart;
import net.minecraft.init.Biomes;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.gen.MapGenBase;
import net.minecraft.world.gen.structure.MapGenStructure;
import net.minecraft.world.gen.structure.MapGenStructureData;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.StructureStart;
import net.minecraft.world.gen.structure.StructureVillagePieces;
import net.minecraft.world.storage.WorldInfo;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Random;

/**
 * Village / mineshaft structure access (formerly the structure half of {@code Reflect}). Vanilla
 * members that are protected or private go through the {@code Accessor*} / {@code Invoker*} mixins in
 * {@code mixin.vanilla}; everything else is a plain call. The signatures keep the {@code Object}
 * typing the callers already use (starts, pieces and map generators arrive from wrapped or modded
 * generators), and a wrong type is a no-op / default, exactly as before.
 *
 * <p>Wrapper unwrapping in {@link #getChunkGenerator} is the one remaining reflection: it walks the
 * fields of unknown third-party generator wrappers.
 */
public final class StructureAccess {

    private static final ThreadLocal<BlockPos.MutableBlockPos> POS =
            ThreadLocal.withInitial(BlockPos.MutableBlockPos::new);

    private StructureAccess() {}

    // ---- biomes / world --------------------------------------------------------------------

    /** Biome at {@code (x, 64, z)} from the provider, plains when the provider has none. */
    public static Biome getBiome(BiomeProvider provider, int x, int z) {
        if (provider == null) return null;
        return provider.getBiome(POS.get().setPos(x, 64, z), Biomes.PLAINS);
    }

    public static BiomeProvider getBiomeProvider(World world) {
        return world != null ? world.getBiomeProvider() : null;
    }

    public static boolean isMapFeaturesEnabled(World world, boolean fallback) {
        if (world == null) return fallback;
        WorldInfo info = world.getWorldInfo();
        return info != null ? info.isMapFeaturesEnabled() : fallback;
    }

    // ---- map generators --------------------------------------------------------------------

    public static World getMapGenWorld(Object mapGen) {
        return mapGen instanceof MapGenBase ? ((AccessorMapGenBase) mapGen).aqtweaks$getWorld() : null;
    }

    public static void setMapGenWorld(Object mapGen, World world) {
        if (mapGen instanceof MapGenBase) {
            ((AccessorMapGenBase) mapGen).aqtweaks$setWorld(world);
        }
    }

    public static Random getMapGenRandom(Object mapGen) {
        return mapGen instanceof MapGenBase ? ((AccessorMapGenBase) mapGen).aqtweaks$getRand() : null;
    }

    public static void initializeStructureData(Object mapGen, World world) {
        if (mapGen instanceof MapGenStructure && world != null) {
            ((AccessorMapGenStructure) mapGen).aqtweaks$initializeStructureData(world);
        }
    }

    public static boolean canSpawnVillage(Object mapGen, int chunkX, int chunkZ) {
        return mapGen instanceof MapGenStructure
                && ((AccessorMapGenStructure) mapGen).aqtweaks$canSpawnStructureAtCoords(chunkX, chunkZ);
    }

    public static Object getStructureStart(Object mapGen, int chunkX, int chunkZ) {
        if (!(mapGen instanceof MapGenStructure)) return null;
        return ((AccessorMapGenStructure) mapGen).aqtweaks$getStructureMap().get(ChunkPos.asLong(chunkX, chunkZ));
    }

    public static boolean hasStructureStart(Object mapGen, int chunkX, int chunkZ) {
        return getStructureStart(mapGen, chunkX, chunkZ) != null;
    }

    /** A snapshot of the start map values, safe to iterate while starts are removed. */
    public static Iterable<Object> getMapGenStructureStarts(Object mapGen) {
        if (!(mapGen instanceof MapGenStructure)) return Collections.emptyList();
        return new ArrayList<Object>(((AccessorMapGenStructure) mapGen).aqtweaks$getStructureMap().values());
    }

    /** Drop a village Start so {@code /locate} and paste cannot keep a rejected well. */
    public static boolean removeStructureStart(Object mapGen, int chunkX, int chunkZ) {
        if (!(mapGen instanceof MapGenStructure)) return false;
        AccessorMapGenStructure acc = (AccessorMapGenStructure) mapGen;
        boolean removed = acc.aqtweaks$getStructureMap().remove(ChunkPos.asLong(chunkX, chunkZ)) != null;
        MapGenStructureData data = acc.aqtweaks$getStructureData();
        if (data != null) {
            data.getTagCompound().removeTag("[" + chunkX + "," + chunkZ + "]");
            data.markDirty();
        }
        return removed;
    }

    /** Persist a modified Start so a reload sees the same boxes. */
    public static void saveMapGenStructureStart(Object mapGen, World world, Object start) {
        if (!(mapGen instanceof MapGenStructure) || world == null || !(start instanceof StructureStart)) return;
        initializeStructureData(mapGen, world);
        StructureStart s = (StructureStart) start;
        MapGenStructureData data = ((AccessorMapGenStructure) mapGen).aqtweaks$getStructureData();
        if (data == null) return;
        int cx = s.getChunkPosX();
        int cz = s.getChunkPosZ();
        NBTTagCompound nbt = s.writeStructureComponentsToNBT(cx, cz);
        data.writeInstance(nbt, cx, cz);
        data.markDirty();
    }

    public static int getVillageDistance(Object mapGen) {
        if (!(mapGen instanceof AccessorMapGenVillage)) return 32;
        int distance = ((AccessorMapGenVillage) mapGen).aqtweaks$getDistance();
        return distance > 0 ? distance : 32;
    }

    public static int getVillageMinDistance(Object mapGen) {
        if (!(mapGen instanceof AccessorMapGenVillage)) return 8;
        int min = ((AccessorMapGenVillage) mapGen).aqtweaks$getMinTownSeparation();
        return min > 0 ? min : 8;
    }

    // ---- starts -----------------------------------------------------------------------------

    public static int getStructureStartChunkX(Object start) {
        return start instanceof StructureStart ? ((StructureStart) start).getChunkPosX() : Integer.MIN_VALUE;
    }

    public static int getStructureStartChunkZ(Object start) {
        return start instanceof StructureStart ? ((StructureStart) start).getChunkPosZ() : Integer.MIN_VALUE;
    }

    /** @return {@code {minX, maxX, minZ, maxZ}} or null */
    public static int[] getStructureStartBoxXZ(Object start) {
        if (!(start instanceof StructureStart)) return null;
        return boxXZ(((StructureStart) start).getBoundingBox());
    }

    public static int getStructureStartMinY(Object start) {
        if (!(start instanceof StructureStart)) return Integer.MIN_VALUE;
        StructureBoundingBox box = ((StructureStart) start).getBoundingBox();
        return box != null ? box.minY : Integer.MIN_VALUE;
    }

    public static int getStructureStartMaxY(Object start) {
        if (!(start instanceof StructureStart)) return Integer.MIN_VALUE;
        StructureBoundingBox box = ((StructureStart) start).getBoundingBox();
        return box != null ? box.maxY : Integer.MIN_VALUE;
    }

    /** A snapshot of the Start's pieces. */
    public static List<Object> getStructureStartComponents(Object start) {
        if (!(start instanceof StructureStart)) return Collections.emptyList();
        return new ArrayList<Object>(((StructureStart) start).getComponents());
    }

    public static boolean addStructureStartComponent(Object start, Object component) {
        if (!(start instanceof StructureStart) || !(component instanceof StructureComponent)) return false;
        ((StructureStart) start).getComponents().add((StructureComponent) component);
        return true;
    }

    /** Recompute the Start's box from its pieces ({@code updateBoundingBox} is protected). */
    public static void updateStructureStartBoundingBox(Object start) {
        if (start instanceof StructureStart) {
            ((InvokerStructureStart) start).aqtweaks$updateBoundingBox();
        }
    }

    public static BiomeProvider getVillageStartBiomeProvider(Object start) {
        return start instanceof StructureVillagePieces.Start
                ? ((AccessorVillageStart) start).aqtweaks$getBiomeProvider() : null;
    }

    /** The 1.12.2 village Start has no World field; callers fall back to the current world stack. */
    public static World getVillageStartWorld(Object start) {
        return null;
    }

    // ---- pieces -----------------------------------------------------------------------------

    /** @return {@code {minX, maxX, minZ, maxZ}} or null; accepts a piece or a Start. */
    public static int[] getStructureComponentBoxXZ(Object component) {
        if (component instanceof StructureComponent) {
            return boxXZ(((StructureComponent) component).getBoundingBox());
        }
        if (component instanceof StructureStart) {
            return boxXZ(((StructureStart) component).getBoundingBox());
        }
        return null;
    }

    /** @return {@code {minY, maxY}} or null */
    public static int[] getStructureComponentMinMaxY(Object component) {
        if (!(component instanceof StructureComponent)) return null;
        StructureBoundingBox box = ((StructureComponent) component).getBoundingBox();
        return box != null ? new int[] {box.minY, box.maxY} : null;
    }

    public static void offsetStructureComponent(Object component, int dx, int dy, int dz) {
        if (!(component instanceof StructureComponent)) return;
        if (dx == 0 && dy == 0 && dz == 0) return;
        ((StructureComponent) component).offset(dx, dy, dz);
    }

    public static void setStructureComponentBoundingBox(Object component, Object box) {
        if (component instanceof StructureComponent && box instanceof StructureBoundingBox) {
            ((AccessorStructureComponent) component).aqtweaks$setBoundingBox((StructureBoundingBox) box);
        }
    }

    private static int[] boxXZ(StructureBoundingBox box) {
        return box != null ? new int[] {box.minX, box.maxX, box.minZ, box.maxZ} : null;
    }

    // ---- chunk generator --------------------------------------------------------------------

    /**
     * The world's chunk generator: the {@code ChunkProviderServer.chunkGenerator}, else the first
     * {@link IChunkGenerator} found by walking wrapper fields (third-party providers).
     */
    public static Object getChunkGenerator(World world) {
        if (world == null) return null;
        IChunkProvider provider = world.getChunkProvider();
        if (provider == null) return null;
        Object direct = chunkGeneratorOf(provider);
        if (direct != null) return direct;
        return nestedChunkGenerator(provider, new IdentityHashMap<>(), 0);
    }

    private static Object chunkGeneratorOf(Object provider) {
        if (provider instanceof ChunkProviderServer) {
            return ((AccessorChunkProviderServer) provider).aqtweaks$getChunkGenerator();
        }
        return null;
    }

    private static Object nestedChunkGenerator(Object node, IdentityHashMap<Object, Boolean> seen, int depth) {
        if (node == null || depth > 8 || seen.containsKey(node)) return null;
        seen.put(node, Boolean.TRUE);
        if (node instanceof IChunkGenerator) return node;
        Object field = chunkGeneratorOf(node);
        if (field != null && field != node) {
            Object found = nestedChunkGenerator(field, seen, depth + 1);
            if (found != null) return found;
        }
        for (Class<?> type = node.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
            Field[] fields;
            try {
                fields = type.getDeclaredFields();
            } catch (Throwable t) {
                continue;
            }
            for (Field f : fields) {
                try {
                    f.setAccessible(true);
                    Object value = f.get(node);
                    if (value == null || value == node) continue;
                    if (value instanceof IChunkGenerator) return value;
                    if (value instanceof IChunkProvider) {
                        Object found = nestedChunkGenerator(value, seen, depth + 1);
                        if (found != null) return found;
                    }
                } catch (Throwable ignored) {}
            }
        }
        return null;
    }
}
