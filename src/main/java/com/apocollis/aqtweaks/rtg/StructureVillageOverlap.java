package com.apocollis.aqtweaks.rtg;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.util.Reflect;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.gen.structure.MapGenVillage;
import net.minecraft.world.gen.structure.template.PlacementSettings;
import net.minecraft.world.gen.structure.template.Template;
import rtg.world.gen.ChunkGeneratorRTG;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Village overlap for post-terrain schematic structures (Astral, Cambion, Mystical huts).
 */
public final class StructureVillageOverlap {

    public static final int RETRY_STEP = 8;
    public static final int RETRY_MAX = 32;

    private StructureVillageOverlap() {}

    public static boolean enabled() {
        return ArcanaQuestTweaksConfig.RtgModuleConfig.surface.skipStructuresOnVillage;
    }

    public static boolean overlapsVillage(World world, BlockPos origin, BlockPos size) {
        if (origin == null || size == null) return false;
        return overlapsVillage(world, origin.getX(), origin.getX() + Math.max(0, size.getX() - 1),
                origin.getZ(), origin.getZ() + Math.max(0, size.getZ() - 1),
                origin.getY(), origin.getY() + Math.max(0, size.getY() - 1));
    }

    public static boolean overlapsVillage(World world, BlockPos origin, BlockPos size, PlacementSettings settings) {
        if (origin == null || size == null) return false;
        int[] box = aabbAfterRotation(origin, size, settings);
        return overlapsVillage(world, box[0], box[1], box[2], box[3], box[4], box[5]);
    }

    public static boolean overlapsVillage(World world, int minX, int maxX, int minZ, int maxZ, int minY, int maxY) {
        if (!enabled() || world == null) return false;
        if (minX > maxX) {
            int t = minX;
            minX = maxX;
            maxX = t;
        }
        if (minZ > maxZ) {
            int t = minZ;
            minZ = maxZ;
            maxZ = t;
        }
        if (minY > maxY) {
            int t = minY;
            minY = maxY;
            maxY = t;
        }

        ensureVillageStarts(world);
        long seed = world.getSeed();
        int heightAbove = Math.max(0, ArcanaQuestTweaksConfig.RtgModuleConfig.surface.villageBoxHeight);

        for (VillagePlate.Record rec : VillagePlate.starts(seed)) {
            if (!VillagePlate.aabbOverlapsVillagePad(minX, maxX, minZ, maxZ, rec)) continue;
            float plate = VillagePlate.resolvePlateOrSample(world, rec);
            if (Float.isNaN(plate)) continue;
            int midY = minY + (maxY - minY) / 2;
            if (!VillagePlate.yInVillageVolume(minY, plate, heightAbove, rec)
                    && !VillagePlate.yInVillageVolume(maxY, plate, heightAbove, rec)
                    && !VillagePlate.yInVillageVolume(midY, plate, heightAbove, rec)) {
                continue;
            }
            return true;
        }
        return false;
    }

    public static int[] aabbAfterRotation(BlockPos origin, BlockPos size, PlacementSettings settings) {
        int x0 = origin.getX();
        int y0 = origin.getY();
        int z0 = origin.getZ();
        int x1 = x0 + Math.max(0, size.getX() - 1);
        int y1 = y0 + Math.max(0, size.getY() - 1);
        int z1 = z0 + Math.max(0, size.getZ() - 1);
        if (settings != null) {
            BlockPos far = Template.transformedBlockPos(settings, new BlockPos(Math.max(0, size.getX() - 1), 0, Math.max(0, size.getZ() - 1)));
            x1 = origin.getX() + far.getX();
            z1 = origin.getZ() + far.getZ();
        }
        return new int[] {
                Math.min(x0, x1), Math.max(x0, x1),
                Math.min(z0, z1), Math.max(z0, z1),
                Math.min(y0, y1), Math.max(y0, y1)
        };
    }

    /**
     * If {@code pos} overlaps a village, walk Chebyshev rings (step 8, max 32) for a dry
     * non-village slot. Preserves {@code origin} Y relative to {@code getHeight}.
     * Returns {@code pos} when skip-on-village is off or there is no overlap.
     * Returns {@code null} when every slot still overlaps (caller should skip paste).
     */
    public static BlockPos relocateOrSkip(World world, Template template, BlockPos pos,
                                          PlacementSettings settings, String name) {
        if (!enabled() || world == null || pos == null || template == null || template.getSize() == null) {
            return pos;
        }
        if (!overlapsVillage(world, pos, template.getSize(), settings)) {
            return pos;
        }
        BlockPos retry = findNearbyLand(world, template, pos, settings);
        if (retry == null) {
            VillageDebug.log("%s skip village overlap at=%d,%d,%d",
                    name == null ? "structure" : name, pos.getX(), pos.getY(), pos.getZ());
            return null;
        }
        VillageDebug.log("%s relocate from=%d,%d,%d to=%d,%d,%d",
                name == null ? "structure" : name,
                pos.getX(), pos.getY(), pos.getZ(),
                retry.getX(), retry.getY(), retry.getZ());
        return retry;
    }

    public static BlockPos findNearbyLand(World world, Template template, BlockPos origin,
                                          PlacementSettings settings) {
        if (world == null || origin == null || template == null || template.getSize() == null) {
            return null;
        }
        int originSurface = Math.max(1, world.getHeight(origin.getX(), origin.getZ()));
        int yOff = origin.getY() - originSurface;
        for (int r = RETRY_STEP; r <= RETRY_MAX; r += RETRY_STEP) {
            for (int dx = -r; dx <= r; dx += RETRY_STEP) {
                for (int dz = -r; dz <= r; dz += RETRY_STEP) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                    int x = origin.getX() + dx;
                    int z = origin.getZ() + dz;
                    if (VillageLandHelper.isNeverRaiseAt(world, x, z)) continue;
                    int y = Math.max(1, world.getHeight(x, z) + yOff);
                    BlockPos candidate = new BlockPos(x, y, z);
                    if (overlapsVillage(world, candidate, template.getSize(), settings)) {
                        continue;
                    }
                    return candidate;
                }
            }
        }
        return null;
    }

    private static void ensureVillageStarts(World world) {
        VillagePlate.ensureStarts(world, findVillageGenerator(world));
    }

    private static final int UNWRAP_DEPTH = 8;
    private static final int UNWRAP_COLLECTION_CAP = 32;

    public static Object findVillageGenerator(World world) {
        MapGenVillage stashed = VillageLandHelper.stashedVillage(world);
        if (stashed != null) return stashed;
        IdentityHashMap<Object, Boolean> seen = new IdentityHashMap<>();
        Object found = findVillageGenerator(Reflect.getChunkGenerator(world), seen, 0);
        if (found != null) return found;
        try {
            return findVillageGenerator(world.getChunkProvider(), seen, 0);
        } catch (Throwable t) {
            return null;
        }
    }

    public static ChunkGeneratorRTG findRtgGenerator(World world) {
        ChunkGeneratorRTG stashed = VillageLandHelper.stashedRtg(world);
        if (stashed != null) return stashed;
        IdentityHashMap<Object, Boolean> seen = new IdentityHashMap<>();
        ChunkGeneratorRTG found = findRtgGenerator(Reflect.getChunkGenerator(world), seen, 0);
        if (found != null) return found;
        try {
            return findRtgGenerator(world.getChunkProvider(), seen, 0);
        } catch (Throwable t) {
            return null;
        }
    }

    private static Object findVillageGenerator(Object node, IdentityHashMap<Object, Boolean> seen, int depth) {
        if (node == null || depth > UNWRAP_DEPTH || seen.containsKey(node) || skipUnwrap(node)) return null;
        seen.put(node, Boolean.TRUE);
        if (node instanceof MapGenVillage) return node;
        Object direct = villageField(node);
        if (direct != null) return direct;
        for (Object nested : nestedUnwrap(node)) {
            Object found = findVillageGenerator(nested, seen, depth + 1);
            if (found != null) return found;
        }
        return null;
    }

    private static ChunkGeneratorRTG findRtgGenerator(Object node, IdentityHashMap<Object, Boolean> seen, int depth) {
        if (node == null || depth > UNWRAP_DEPTH || seen.containsKey(node) || skipUnwrap(node)) return null;
        seen.put(node, Boolean.TRUE);
        if (node instanceof ChunkGeneratorRTG) return (ChunkGeneratorRTG) node;
        for (Object nested : nestedUnwrap(node)) {
            ChunkGeneratorRTG found = findRtgGenerator(nested, seen, depth + 1);
            if (found != null) return found;
        }
        return null;
    }

    private static Object villageField(Object chunkGen) {
        for (Class<?> type = chunkGen.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
            Field[] fields;
            try {
                fields = type.getDeclaredFields();
            } catch (Throwable t) {
                continue;
            }
            for (Field field : fields) {
                try {
                    field.setAccessible(true);
                    Object value = field.get(chunkGen);
                    if (value instanceof MapGenVillage) return value;
                } catch (Throwable ignored) {}
            }
        }
        return null;
    }

    private static boolean skipUnwrap(Object value) {
        if (value instanceof World) return true;
        if (value instanceof String || value instanceof Number || value instanceof Boolean) return true;
        if (value instanceof Class || value instanceof Enum) return true;
        Class<?> type = value.getClass();
        if (type.isPrimitive()) return true;
        String name = type.getName();
        return name.startsWith("net.minecraft.entity.")
                || name.startsWith("net.minecraft.block.")
                || name.startsWith("net.minecraft.world.chunk.Chunk")
                || name.contains("BiomeProvider");
    }

    private static List<Object> nestedUnwrap(Object node) {
        List<Object> out = new ArrayList<>();
        for (Class<?> type = node.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
            Field[] fields;
            try {
                fields = type.getDeclaredFields();
            } catch (Throwable t) {
                continue;
            }
            for (Field field : fields) {
                try {
                    field.setAccessible(true);
                    Object value = field.get(node);
                    addUnwrapChild(out, node, value);
                } catch (Throwable ignored) {}
            }
        }
        return out;
    }

    private static void addUnwrapChild(List<Object> out, Object parent, Object value) {
        if (value == null || value == parent || skipUnwrap(value)) return;
        if (value instanceof MapGenVillage) return;
        if (value instanceof IChunkGenerator || value instanceof IChunkProvider || value instanceof ChunkGeneratorRTG) {
            out.add(value);
            return;
        }
        if (value instanceof Collection<?> collection) {
            int n = 0;
            for (Object item : collection) {
                if (n++ >= UNWRAP_COLLECTION_CAP) break;
                addUnwrapChild(out, parent, item);
            }
            return;
        }
        if (value instanceof Map<?, ?> map) {
            int n = 0;
            for (Object item : map.values()) {
                if (n++ >= UNWRAP_COLLECTION_CAP) break;
                addUnwrapChild(out, parent, item);
            }
            return;
        }
        String name = value.getClass().getName().toLowerCase();
        if (name.contains("chunkgenerator") || name.contains("chunkprovider") || name.contains("wrapped")) {
            out.add(value);
        }
    }
}
