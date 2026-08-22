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
        public final List<int[]> shrineBoxes;
        public final int wellX;
        public final int wellZ;
        public final int minY;
        public final int maxY;

        private Record(Object start, int[] xz, List<int[]> landBoxes, List<int[]> buildingBoxes,
                       List<int[]> shrineBoxes, int wellX, int wellZ, int minY, int maxY) {
            this.start = start;
            this.xz = xz;
            this.landBoxes = landBoxes;
            this.buildingBoxes = buildingBoxes;
            this.shrineBoxes = shrineBoxes;
            this.wellX = wellX;
            this.wellZ = wellZ;
            this.minY = minY;
            this.maxY = maxY;
        }

        public List<int[]> landBoxesOrStart() {
            if (landBoxes != null && !landBoxes.isEmpty()) return landBoxes;
            return xz == null ? Collections.emptyList() : Collections.singletonList(xz);
        }

        public List<int[]> landBoxesOrEmpty() {
            return landBoxes != null ? landBoxes : Collections.emptyList();
        }

        public List<int[]> buildingBoxesOrEmpty() {
            return buildingBoxes != null ? buildingBoxes : Collections.emptyList();
        }

        public List<int[]> shrineBoxesOrEmpty() {
            return shrineBoxes != null ? shrineBoxes : Collections.emptyList();
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
        int wellX = chunkX > Integer.MIN_VALUE ? chunkX * 16 + 2 : (xz[0] + xz[1]) >> 1;
        int wellZ = chunkZ > Integer.MIN_VALUE ? chunkZ * 16 + 2 : (xz[2] + xz[3]) >> 1;
        if (world != null && chunkX > Integer.MIN_VALUE) {
            int[] resolved = VillageLandHelper.resolvedWellXZ(world, wellX, wellZ);
            wellX = resolved[0];
            wellZ = resolved[1];
        }
        remember(world, start, chunkX, chunkZ, wellX, wellZ);
    }

    public static void remember(World world, Object start, int chunkX, int chunkZ, int wellX, int wellZ) {
        if (start == null) return;
        int[] xz = Reflect.getStructureStartBoxXZ(start);
        if (xz == null) return;
        long seed = world != null ? Reflect.getSeed(world) : 0L;
        int minY = Reflect.getStructureStartMinY(start);
        int maxY = Reflect.getStructureStartMaxY(start);
        List<int[]> landBoxes = landBoxesOf(start);
        List<int[]> buildingBoxes = buildingBoxesOf(start);
        List<int[]> shrineBoxes = shrineBoxesOf(start);
        List<Record> list = STARTS.computeIfAbsent(seed, k -> Collections.synchronizedList(new ArrayList<>()));
        synchronized (list) {
            String id = key(seed, xz);
            Record rec = new Record(start, xz, landBoxes, buildingBoxes, shrineBoxes, wellX, wellZ, minY, maxY);
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
        if (mapGen instanceof net.minecraft.world.gen.structure.MapGenVillage) {
            VillageLandHelper.forgetRejectedStarts(
                    (net.minecraft.world.gen.structure.MapGenVillage) mapGen, world);
        }
        for (Object start : Reflect.getMapGenStructureStarts(mapGen)) {
            remember(world, start);
        }
    }

    public static void forget(World world, Object start, int chunkX, int chunkZ) {
        long seed = world != null ? Reflect.getSeed(world) : 0L;
        int[] xz = start != null ? Reflect.getStructureStartBoxXZ(start) : null;
        int wellX = chunkX > Integer.MIN_VALUE ? chunkX * 16 + 2 : Integer.MIN_VALUE;
        int wellZ = chunkZ > Integer.MIN_VALUE ? chunkZ * 16 + 2 : Integer.MIN_VALUE;
        List<Record> list = STARTS.get(seed);
        if (list != null) {
            synchronized (list) {
                list.removeIf(rec -> {
                    if (xz != null && rec.xz != null && key(seed, rec.xz).equals(key(seed, xz))) {
                        return true;
                    }
                    return rec.wellX == wellX && rec.wellZ == wellZ;
                });
            }
        }
        if (xz != null) {
            HEIGHTS.remove(key(seed, xz));
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
            for (int[] box : rec.landBoxesOrEmpty()) {
                if (overlapsXZ(box, chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ, e)) {
                    out.add(rec);
                    break;
                }
            }
        }
        return out;
    }

    private static boolean overlapsXZ(int[] box, int chunkMinX, int chunkMaxX, int chunkMinZ, int chunkMaxZ, int extra) {
        if (box == null) return false;
        if (box[1] + extra < chunkMinX || box[0] - extra > chunkMaxX) return false;
        if (box[3] + extra < chunkMinZ || box[2] - extra > chunkMaxZ) return false;
        return true;
    }

    public static List<int[]> overlappingXZ(long seed, int chunkMinX, int chunkMaxX, int chunkMinZ, int chunkMaxZ, int extra) {
        List<int[]> out = new ArrayList<>();
        for (Record rec : overlappingRecords(seed, chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ, extra)) {
            out.addAll(rec.landBoxesOrEmpty());
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

    public static boolean yInVillageVolume(int y, float plateHeight, int heightAbove, Record rec) {
        int padY = Math.round(plateHeight);
        int maxY = padY + Math.max(0, heightAbove);
        int floor = wellFloorY(rec, padY);
        return y >= floor && y <= maxY;
    }

    /**
     * Detection floor: snapped well-piece {@code minY}, or {@code plate - wellHeight} if still the
     * unsnapped template box (64..78).
     */
    public static int wellFloorY(Record rec, int plateY) {
        int[] wellY = wellPieceMinMaxY(rec != null ? rec.start : null);
        if (wellY == null) {
            return plateY - 14;
        }
        int minY = wellY[0];
        int maxY = wellY[1];
        if (minY == 64 && maxY == 78) {
            return plateY - (maxY - minY);
        }
        return Math.min(minY, plateY);
    }

    private static int[] wellPieceMinMaxY(Object start) {
        if (start == null) return null;
        for (Object piece : Reflect.getStructureStartComponents(start)) {
            if (VillageLandHelper.isVillageWellOrStart(piece)) {
                int[] y = Reflect.getStructureComponentMinMaxY(piece);
                if (y != null) return y;
            }
        }
        return null;
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
            if (VillageLandHelper.isVillageRoad(piece) && VillageLandHelper.isAabbFullyFlooded(start, piece)) {
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

    public static List<int[]> shrineBoxesOf(Object start) {
        List<int[]> out = new ArrayList<>();
        for (Object piece : Reflect.getStructureStartComponents(start)) {
            if (!(piece instanceof VillagePieceAstralSmallShrine)) continue;
            int[] box = Reflect.getStructureComponentBoxXZ(piece);
            if (box != null) out.add(box);
        }
        return out;
    }

    public static boolean sameXZ(int[] a, int[] b) {
        return a != null && b != null
                && a[0] == b[0] && a[1] == b[1] && a[2] == b[2] && a[3] == b[3];
    }

    public static boolean containsXZBox(List<int[]> boxes, int[] box) {
        if (boxes == null || box == null) return false;
        for (int[] candidate : boxes) {
            if (sameXZ(candidate, box)) return true;
        }
        return false;
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
