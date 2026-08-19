package com.apocollis.aqtweaks.rtg;

import com.apocollis.aqtweaks.util.Reflect;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Village start AABBs and pad height: flatten writes, structure detection reads.
 */
public final class VillagePlate {

    private static final Map<String, Float> HEIGHTS = new ConcurrentHashMap<>();
    private static final Map<Long, List<Record>> STARTS = new ConcurrentHashMap<>();

    public static final class Record {
        public final Object start;
        public final int[] xz;
        public final int minY;
        public final int maxY;

        private Record(Object start, int[] xz, int minY, int maxY) {
            this.start = start;
            this.xz = xz;
            this.minY = minY;
            this.maxY = maxY;
        }
    }

    private VillagePlate() {}

    public static String key(long seed, int[] box) {
        return seed + ":" + box[0] + "," + box[1] + "," + box[2] + "," + box[3];
    }

    public static void put(long seed, int[] box, float height) {
        if (box == null) return;
        HEIGHTS.put(key(seed, box), height);
    }

    public static Float get(long seed, int[] box) {
        if (box == null) return null;
        return HEIGHTS.get(key(seed, box));
    }

    public static void remember(World world, Object start) {
        if (start == null) return;
        int[] xz = Reflect.getStructureStartBoxXZ(start);
        if (xz == null) return;
        long seed = world != null ? Reflect.getSeed(world) : 0L;
        int minY = Reflect.getStructureStartMinY(start);
        int maxY = Reflect.getStructureStartMaxY(start);
        List<Record> list = STARTS.computeIfAbsent(seed, k -> Collections.synchronizedList(new ArrayList<>()));
        synchronized (list) {
            String id = key(seed, xz);
            for (int i = 0; i < list.size(); i++) {
                Record existing = list.get(i);
                if (id.equals(key(seed, existing.xz))) {
                    list.set(i, new Record(start, xz, minY, maxY));
                    return;
                }
            }
            list.add(new Record(start, xz, minY, maxY));
        }
    }

    public static void rememberAll(World world, Object mapGen) {
        if (mapGen == null) return;
        Reflect.initializeStructureData(mapGen, world);
        for (Object start : Reflect.getMapGenStructureStarts(mapGen)) {
            remember(world, start);
        }
    }

    public static List<Record> starts(long seed) {
        List<Record> list = STARTS.get(seed);
        if (list == null || list.isEmpty()) return Collections.emptyList();
        synchronized (list) {
            return new ArrayList<>(list);
        }
    }

    public static List<int[]> overlappingXZ(long seed, int chunkMinX, int chunkMaxX, int chunkMinZ, int chunkMaxZ, int extra) {
        List<int[]> out = new ArrayList<>();
        int e = Math.max(0, extra);
        for (Record rec : starts(seed)) {
            if (rec.xz[1] + e < chunkMinX || rec.xz[0] - e > chunkMaxX) continue;
            if (rec.xz[3] + e < chunkMinZ || rec.xz[2] - e > chunkMaxZ) continue;
            out.add(rec.xz);
        }
        return out;
    }

    public static int[] padded(int[] box, int pad) {
        if (box == null) return null;
        if (pad <= 0) return box;
        return new int[] {box[0] - pad, box[1] + pad, box[2] - pad, box[3] + pad};
    }

    public static boolean containsXZ(int x, int z, int[] box) {
        return box != null && x >= box[0] && x <= box[1] && z >= box[2] && z <= box[3];
    }

    public static boolean yInSlab(int y, float plateHeight, int heightAbove) {
        int padY = Math.round(plateHeight);
        int maxY = padY + Math.max(0, heightAbove);
        return y >= padY && y <= maxY;
    }

    public static boolean yInStartVolume(int y, Record rec, float plateOrNaN, int heightAbove) {
        if (!Float.isNaN(plateOrNaN)) {
            return yInSlab(y, plateOrNaN, heightAbove);
        }
        int minY = rec.minY > Integer.MIN_VALUE ? rec.minY : 0;
        int maxY = rec.maxY > Integer.MIN_VALUE ? rec.maxY : minY;
        return y >= minY && y <= maxY + Math.max(0, heightAbove);
    }

    /**
     * Cached plate height, or NaN if this village was never flattened this session.
     */
    public static float resolvePlate(World world, int[] box) {
        long seed = world != null ? Reflect.getSeed(world) : 0L;
        Float cached = get(seed, box);
        return cached != null ? cached : Float.NaN;
    }

    public static float resolve(World world, Object start, int[] box) {
        float cached = resolvePlate(world, box);
        if (!Float.isNaN(cached)) return cached;

        float height = sampleWorldSurface(world, box);
        if (Float.isNaN(height)) {
            int minY = Reflect.getStructureStartMinY(start);
            height = minY > Integer.MIN_VALUE ? minY : 64.0F;
        }
        put(world != null ? Reflect.getSeed(world) : 0L, box, height);
        return height;
    }

    private static float sampleWorldSurface(World world, int[] box) {
        if (world == null || box == null) return Float.NaN;
        int x = (box[0] + box[1]) >> 1;
        int z = (box[2] + box[3]) >> 1;
        try {
            int y = world.getHeight(x, z);
            if (y > 0) return y;
        } catch (Throwable ignored) {}
        return Float.NaN;
    }
}
