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
        public final List<int[]> landBoxes;
        public final List<int[]> buildingBoxes;
        public final int wellX;
        public final int wellZ;
        public final int minY;
        public final int maxY;

        private Record(Object start, int[] xz, List<int[]> landBoxes, List<int[]> buildingBoxes,
                       int wellX, int wellZ, int minY, int maxY) {
            this.start = start;
            this.xz = xz;
            this.landBoxes = landBoxes;
            this.buildingBoxes = buildingBoxes;
            this.wellX = wellX;
            this.wellZ = wellZ;
            this.minY = minY;
            this.maxY = maxY;
        }

        public List<int[]> landBoxesOrStart() {
            if (landBoxes != null && !landBoxes.isEmpty()) return landBoxes;
            return xz == null ? Collections.emptyList() : Collections.singletonList(xz);
        }

        public List<int[]> buildingBoxesOrEmpty() {
            return buildingBoxes != null ? buildingBoxes : Collections.emptyList();
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
        int cx = Reflect.getStructureStartChunkX(start);
        int cz = Reflect.getStructureStartChunkZ(start);
        remember(world, start, cx, cz);
    }

    public static void remember(World world, Object start, int chunkX, int chunkZ) {
        if (start == null) return;
        int[] xz = Reflect.getStructureStartBoxXZ(start);
        if (xz == null) return;
        long seed = world != null ? Reflect.getSeed(world) : 0L;
        int minY = Reflect.getStructureStartMinY(start);
        int maxY = Reflect.getStructureStartMaxY(start);
        int wellX = chunkX > Integer.MIN_VALUE ? chunkX * 16 + 2 : (xz[0] + xz[1]) >> 1;
        int wellZ = chunkZ > Integer.MIN_VALUE ? chunkZ * 16 + 2 : (xz[2] + xz[3]) >> 1;
        List<int[]> landBoxes = landBoxesOf(start);
        List<int[]> buildingBoxes = buildingBoxesOf(start);
        List<Record> list = STARTS.computeIfAbsent(seed, k -> Collections.synchronizedList(new ArrayList<>()));
        synchronized (list) {
            String id = key(seed, xz);
            Record rec = new Record(start, xz, landBoxes, buildingBoxes, wellX, wellZ, minY, maxY);
            for (int i = 0; i < list.size(); i++) {
                Record existing = list.get(i);
                if (id.equals(key(seed, existing.xz))) {
                    list.set(i, rec);
                    return;
                }
            }
            list.add(rec);
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

    public static List<Record> overlappingStartAabb(long seed, int chunkMinX, int chunkMaxX, int chunkMinZ, int chunkMaxZ, int extra) {
        List<Record> out = new ArrayList<>();
        int e = Math.max(0, extra);
        for (Record rec : starts(seed)) {
            if (rec.xz == null) continue;
            if (rec.xz[1] + e < chunkMinX || rec.xz[0] - e > chunkMaxX) continue;
            if (rec.xz[3] + e < chunkMinZ || rec.xz[2] - e > chunkMaxZ) continue;
            out.add(rec);
        }
        return out;
    }

    public static List<Record> overlappingRecords(long seed, int chunkMinX, int chunkMaxX, int chunkMinZ, int chunkMaxZ, int extra) {
        List<Record> out = new ArrayList<>();
        int e = Math.max(0, extra);
        for (Record rec : starts(seed)) {
            for (int[] box : rec.landBoxesOrStart()) {
                if (box[1] + e < chunkMinX || box[0] - e > chunkMaxX) continue;
                if (box[3] + e < chunkMinZ || box[2] - e > chunkMaxZ) continue;
                out.add(rec);
                break;
            }
        }
        return out;
    }

    public static List<int[]> overlappingXZ(long seed, int chunkMinX, int chunkMaxX, int chunkMinZ, int chunkMaxZ, int extra) {
        List<int[]> out = new ArrayList<>();
        for (Record rec : overlappingRecords(seed, chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ, extra)) {
            out.addAll(rec.landBoxesOrStart());
        }
        return out;
    }

    public static int[] padded(int[] box, int pad) {
        if (box == null) return null;
        if (pad <= 0) return box;
        return new int[] {box[0] - pad, box[1] + pad, box[2] - pad, box[3] + pad};
    }

    public static int[] union(List<int[]> boxes) {
        if (boxes == null || boxes.isEmpty()) return null;
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (int[] box : boxes) {
            if (box == null) continue;
            minX = Math.min(minX, box[0]);
            maxX = Math.max(maxX, box[1]);
            minZ = Math.min(minZ, box[2]);
            maxZ = Math.max(maxZ, box[3]);
        }
        if (minX > maxX) return null;
        return new int[] {minX, maxX, minZ, maxZ};
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
        int extra = Math.max(0, heightAbove);
        int startMin = rec.minY > Integer.MIN_VALUE ? rec.minY : 0;
        int startMax = rec.maxY > Integer.MIN_VALUE ? rec.maxY : startMin;
        int floor = Math.min(startMin, 63);
        if (!Float.isNaN(plateOrNaN)) {
            floor = Math.min(floor, Math.round(plateOrNaN));
            startMax = Math.max(startMax, Math.round(plateOrNaN));
        }
        return y >= floor && y <= startMax + extra;
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

    public static List<int[]> landBoxesOf(Object start) {
        List<int[]> out = new ArrayList<>();
        for (Object piece : Reflect.getStructureStartComponents(start)) {
            int[] box = Reflect.getStructureComponentBoxXZ(piece);
            if (box == null) continue;
            if (VillageLandHelper.isVillageRoad(piece) && VillageLandHelper.isAabbFlooded(start, piece)) {
                continue;
            }
            out.add(box);
        }
        return out;
    }

    /**
     * Houses, RC, and the well. Roads and docks are excluded so swamp raise stays rounded around buildings.
     */
    public static List<int[]> buildingBoxesOf(Object start) {
        List<int[]> out = new ArrayList<>();
        for (Object piece : Reflect.getStructureStartComponents(start)) {
            int[] box = Reflect.getStructureComponentBoxXZ(piece);
            if (box == null || VillageLandHelper.isVillageRoad(piece)) continue;
            out.add(box);
        }
        return out;
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
