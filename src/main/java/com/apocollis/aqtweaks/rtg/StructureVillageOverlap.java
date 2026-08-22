package com.apocollis.aqtweaks.rtg;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.util.Reflect;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.gen.structure.MapGenVillage;
import net.minecraft.world.gen.structure.template.PlacementSettings;
import net.minecraft.world.gen.structure.template.Template;
import rtg.world.gen.ChunkGeneratorRTG;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

/**
 * Village overlap for post-terrain schematic structures (Astral, Cambion, Mystical huts).
 */
public final class StructureVillageOverlap {

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
        long seed = Reflect.getSeed(world);
        int pad = Math.max(0, ArcanaQuestTweaksConfig.RtgModuleConfig.surface.villageBoxXZPad);
        int heightAbove = Math.max(0, ArcanaQuestTweaksConfig.RtgModuleConfig.surface.villageBoxHeight);

        for (VillagePlate.Record rec : VillagePlate.starts(seed)) {
            float plate = VillagePlate.resolvePlate(world, rec.xz);
            if (Float.isNaN(plate)) continue;
            int midY = minY + (maxY - minY) / 2;
            if (!VillagePlate.yInVillageVolume(minY, plate, heightAbove, rec)
                    && !VillagePlate.yInVillageVolume(maxY, plate, heightAbove, rec)
                    && !VillagePlate.yInVillageVolume(midY, plate, heightAbove, rec)) {
                continue;
            }
            boolean xzHit = false;
            for (int[] box : rec.landBoxesOrEmpty()) {
                int[] padded = VillagePlate.padded(box, pad);
                if (xzIntersects(padded, minX, maxX, minZ, maxZ)) {
                    xzHit = true;
                    break;
                }
            }
            if (xzHit) return true;
        }

        IChunkProvider provider;
        try {
            provider = world.getChunkProvider();
        } catch (Throwable t) {
            return false;
        }
        if (!(provider instanceof ChunkProviderServer)) return false;
        ChunkProviderServer server = (ChunkProviderServer) provider;
        int midY = Math.max(1, minY + (maxY - minY) / 2);
        int[] xs = new int[] {minX, maxX, (minX + maxX) >> 1};
        int[] zs = new int[] {minZ, maxZ, (minZ + maxZ) >> 1};
        for (int x : xs) {
            for (int z : zs) {
                try {
                    if (server.isInsideStructure(world, "Village", new BlockPos(x, midY, z))) {
                        return true;
                    }
                } catch (Throwable ignored) {}
            }
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

    private static boolean xzIntersects(int[] box, int minX, int maxX, int minZ, int maxZ) {
        if (box == null) return false;
        return box[0] <= maxX && box[1] >= minX && box[2] <= maxZ && box[3] >= minZ;
    }

    private static void ensureVillageStarts(World world) {
        long seed = Reflect.getSeed(world);
        if (!VillagePlate.starts(seed).isEmpty()) return;
        Object villageGen = findVillageGenerator(world);
        if (villageGen != null) {
            VillagePlate.rememberAll(world, villageGen);
        }
    }

    public static Object findVillageGenerator(World world) {
        MapGenVillage stashed = VillageLandHelper.stashedVillage(world);
        if (stashed != null) return stashed;
        return findVillageGenerator(Reflect.getChunkGenerator(world), new IdentityHashMap<>());
    }

    public static ChunkGeneratorRTG findRtgGenerator(World world) {
        ChunkGeneratorRTG stashed = VillageLandHelper.stashedRtg(world);
        if (stashed != null) return stashed;
        return findRtgGenerator(Reflect.getChunkGenerator(world), new IdentityHashMap<>());
    }

    private static Object findVillageGenerator(Object chunkGen, IdentityHashMap<Object, Boolean> seen) {
        if (chunkGen == null || seen.containsKey(chunkGen)) return null;
        seen.put(chunkGen, Boolean.TRUE);
        if (chunkGen instanceof MapGenVillage) return chunkGen;
        Object direct = villageField(chunkGen);
        if (direct != null) return direct;
        for (Object nested : nestedChunkGenerators(chunkGen)) {
            Object found = findVillageGenerator(nested, seen);
            if (found != null) return found;
        }
        return null;
    }

    private static ChunkGeneratorRTG findRtgGenerator(Object chunkGen, IdentityHashMap<Object, Boolean> seen) {
        if (chunkGen == null || seen.containsKey(chunkGen)) return null;
        seen.put(chunkGen, Boolean.TRUE);
        if (chunkGen instanceof ChunkGeneratorRTG) return (ChunkGeneratorRTG) chunkGen;
        for (Object nested : nestedChunkGenerators(chunkGen)) {
            ChunkGeneratorRTG found = findRtgGenerator(nested, seen);
            if (found != null) return found;
        }
        return null;
    }

    private static Object villageField(Object chunkGen) {
        for (Class<?> type = chunkGen.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                try {
                    field.setAccessible(true);
                    Object value = field.get(chunkGen);
                    if (value instanceof MapGenVillage) return value;
                } catch (Throwable ignored) {}
            }
        }
        return null;
    }

    private static List<Object> nestedChunkGenerators(Object chunkGen) {
        List<Object> out = new ArrayList<>();
        for (Class<?> type = chunkGen.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                try {
                    field.setAccessible(true);
                    Object value = field.get(chunkGen);
                    if (value == null || value == chunkGen) continue;
                    if (value instanceof IChunkGenerator || value instanceof ChunkGeneratorRTG) {
                        out.add(value);
                    }
                } catch (Throwable ignored) {}
            }
        }
        return out;
    }
}
