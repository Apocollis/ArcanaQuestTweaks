package com.apocollis.aqtweaks.rtg;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.util.Reflect;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureVillagePieces;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
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
        public final int startChunkX;
        public final int startChunkZ;
        /** Mixin well-walk or an AABB-miss refresh already rebuilt boxes from the live Start. */
        public final boolean landBoxesLocked;

        private Record(Object start, int[] xz, List<int[]> landBoxes, List<int[]> buildingBoxes,
                       List<int[]> shrineBoxes, int wellX, int wellZ, int minY, int maxY,
                       int startChunkX, int startChunkZ, boolean landBoxesLocked) {
            this.start = start;
            this.xz = xz;
            this.landBoxes = landBoxes;
            this.buildingBoxes = buildingBoxes;
            this.shrineBoxes = shrineBoxes;
            this.wellX = wellX;
            this.wellZ = wellZ;
            this.minY = minY;
            this.maxY = maxY;
            this.startChunkX = startChunkX;
            this.startChunkZ = startChunkZ;
            this.landBoxesLocked = landBoxesLocked;
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

    public static String wellKey(long seed, int chunkX, int chunkZ) {
        return seed + ":c:" + chunkX + "," + chunkZ;
    }

    public static String wellKey(long seed, Record rec) {
        if (rec == null) return seed + ":c:?,?";
        return wellKey(seed, rec.startChunkX, rec.startChunkZ);
    }

    public static void put(long seed, Record rec, float height) {
        if (rec == null) return;
        HEIGHTS.put(wellKey(seed, rec), height);
    }

    public static Float get(long seed, Record rec) {
        if (rec == null) return null;
        return HEIGHTS.get(wellKey(seed, rec));
    }

    public static void remember(World world, Object start) {
        remember(world, start, false);
    }

    public static void remember(World world, Object start, int chunkX, int chunkZ) {
        rememberResolved(world, start, chunkX, chunkZ, true);
    }

    /**
     * Mixin well-walk path: replace any Record for this well chunk so walked well XZ wins.
     */
    public static void remember(World world, Object start, int chunkX, int chunkZ, int wellX, int wellZ) {
        putRecord(world, start, chunkX, chunkZ, wellX, wellZ, true);
    }

    public static void rememberIfAbsent(World world, Object start) {
        remember(world, start, false);
    }

    private static void remember(World world, Object start, boolean replace) {
        int cx = Reflect.getStructureStartChunkX(start);
        int cz = Reflect.getStructureStartChunkZ(start);
        rememberResolved(world, start, cx, cz, replace);
    }

    private static void rememberResolved(World world, Object start, int chunkX, int chunkZ, boolean replace) {
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
        putRecord(world, start, chunkX, chunkZ, wellX, wellZ, replace);
    }

    private static void putRecord(World world, Object start, int chunkX, int chunkZ,
                                  int wellX, int wellZ, boolean replace) {
        if (start == null) return;
        int[] xz = Reflect.getStructureStartBoxXZ(start);
        if (xz == null) return;
        int startChunkX = chunkX > Integer.MIN_VALUE ? chunkX : Reflect.getStructureStartChunkX(start);
        int startChunkZ = chunkZ > Integer.MIN_VALUE ? chunkZ : Reflect.getStructureStartChunkZ(start);
        if (startChunkX == Integer.MIN_VALUE) startChunkX = wellX >> 4;
        if (startChunkZ == Integer.MIN_VALUE) startChunkZ = wellZ >> 4;
        long seed = world != null ? Reflect.getSeed(world) : 0L;
        List<Record> list = STARTS.computeIfAbsent(seed, k -> Collections.synchronizedList(new ArrayList<>()));
        synchronized (list) {
            for (int i = 0; i < list.size(); i++) {
                Record existing = list.get(i);
                if (existing.startChunkX == startChunkX && existing.startChunkZ == startChunkZ) {
                    if (!replace) return;
                    int minY = Reflect.getStructureStartMinY(start);
                    int maxY = Reflect.getStructureStartMaxY(start);
                    list.set(i, new Record(start, xz, landBoxesOf(start), buildingBoxesOf(start),
                            shrineBoxesOf(start), wellX, wellZ, minY, maxY,
                            startChunkX, startChunkZ, true));
                    return;
                }
            }
            int minY = Reflect.getStructureStartMinY(start);
            int maxY = Reflect.getStructureStartMaxY(start);
            list.add(new Record(start, xz, landBoxesOf(start), buildingBoxesOf(start),
                    shrineBoxesOf(start), wellX, wellZ, minY, maxY,
                    startChunkX, startChunkZ, replace));
        }
    }

    /**
     * Backfill from vanilla {@code structureMap} when Tweaks' list is empty (world load).
     * Do not call on every chunk that is simply not a village.
     */
    public static void ensureStarts(World world, Object mapGen) {
        if (world == null || mapGen == null) return;
        if (!starts(Reflect.getSeed(world)).isEmpty()) return;
        rememberAll(world, mapGen);
    }

    /**
     * Recover Starts for nearby vanilla well chunks only. Does not walk every village in the world.
     */
    public static void rememberNearby(World world, Object mapGen, int cx, int cz) {
        if (world == null || mapGen == null) return;
        Reflect.initializeStructureData(mapGen, world);
        int spacing = Reflect.getVillageDistance(mapGen);
        if (spacing < 9) spacing = 32;
        int minTown = Reflect.getVillageMinDistance(mapGen);
        if (minTown < 1 || minTown >= spacing) minTown = 8;
        long seed = Reflect.getSeed(world);
        int minCellX = VillageLandHelper.villageCell(cx - VillageLandHelper.VILLAGE_LAYOUT_RADIUS, spacing);
        int maxCellX = VillageLandHelper.villageCell(cx + VillageLandHelper.VILLAGE_LAYOUT_RADIUS, spacing);
        int minCellZ = VillageLandHelper.villageCell(cz - VillageLandHelper.VILLAGE_LAYOUT_RADIUS, spacing);
        int maxCellZ = VillageLandHelper.villageCell(cz + VillageLandHelper.VILLAGE_LAYOUT_RADIUS, spacing);
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
        for (int cellX = minCellX; cellX <= maxCellX; cellX++) {
            for (int cellZ = minCellZ; cellZ <= maxCellZ; cellZ++) {
                int[] well = VillageLandHelper.villageWellChunk(seed, cellX, cellZ, spacing, minTown);
                Object start = Reflect.getStructureStart(mapGen, well[0], well[1]);
                if (start != null) {
                    rememberIfAbsent(world, start);
                }
            }
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
            rememberIfAbsent(world, start);
        }
    }

    public static void forget(World world, Object start, int chunkX, int chunkZ) {
        long seed = world != null ? Reflect.getSeed(world) : 0L;
        int startChunkX = chunkX > Integer.MIN_VALUE ? chunkX : Reflect.getStructureStartChunkX(start);
        int startChunkZ = chunkZ > Integer.MIN_VALUE ? chunkZ : Reflect.getStructureStartChunkZ(start);
        int[] xz = start != null ? Reflect.getStructureStartBoxXZ(start) : null;
        int wellX = chunkX > Integer.MIN_VALUE ? chunkX * 16 + 2 : Integer.MIN_VALUE;
        int wellZ = chunkZ > Integer.MIN_VALUE ? chunkZ * 16 + 2 : Integer.MIN_VALUE;
        List<Record> list = STARTS.get(seed);
        if (list != null) {
            synchronized (list) {
                list.removeIf(rec -> {
                    boolean match = (startChunkX > Integer.MIN_VALUE && rec.startChunkX == startChunkX
                            && rec.startChunkZ == startChunkZ)
                            || (xz != null && rec.xz != null && key(seed, rec.xz).equals(key(seed, xz)))
                            || (rec.wellX == wellX && rec.wellZ == wellZ);
                    if (match) {
                        HEIGHTS.remove(wellKey(seed, rec));
                    }
                    return match;
                });
            }
        }
        if (startChunkX > Integer.MIN_VALUE) {
            HEIGHTS.remove(wellKey(seed, startChunkX, startChunkZ));
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

    /**
     * Land-box hits plus start-AABB villages whose live boxes now overlap this chunk.
     * Does not flatten the start AABB as a hull. Rebuilds boxes at most once per village
     * when the snapshot looks thin (world-load rememberIfAbsent) or unlocked.
     */
    public static List<Record> mergeStartAabbHits(World world, List<Record> landHits,
                                                 int chunkMinX, int chunkMaxX, int chunkMinZ, int chunkMaxZ, int extra) {
        long seed = world != null ? Reflect.getSeed(world) : 0L;
        List<Record> aabbHits = overlappingStartAabb(seed, chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ, extra);
        if ((landHits == null || landHits.isEmpty()) && aabbHits.isEmpty()) {
            return Collections.emptyList();
        }
        LinkedHashMap<String, Record> byWell = new LinkedHashMap<>();
        if (landHits != null) {
            for (Record rec : landHits) {
                byWell.put(wellKey(seed, rec), rec);
            }
        }
        for (Record rec : aabbHits) {
            String id = wellKey(seed, rec);
            if (byWell.containsKey(id)) continue;
            Record refreshed = maybeRefreshLandBoxes(seed, rec);
            if (landOverlapsChunk(refreshed, chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ, extra)) {
                byWell.put(id, refreshed);
            }
        }
        return new ArrayList<>(byWell.values());
    }

    /**
     * Rebuild unlocked land boxes once (world-load {@code rememberIfAbsent} snapshots).
     * Locked Records (layout remember / prior refresh) are left as-is.
     */
    public static List<Record> refreshUnlocked(long seed, List<Record> hits) {
        if (hits == null || hits.isEmpty()) {
            return hits == null ? Collections.emptyList() : hits;
        }
        List<Record> out = new ArrayList<>(hits.size());
        for (Record rec : hits) {
            out.add(maybeRefreshLandBoxes(seed, rec));
        }
        return out;
    }

    private static Record maybeRefreshLandBoxes(long seed, Record rec) {
        if (rec == null || rec.start == null) return rec;
        if (rec.landBoxesLocked) return rec;
        Record next = replaceBoxes(seed, rec);
        if (next != rec && VillageDebug.once("refreshBoxes:" + wellKey(seed, next))) {
            VillageDebug.log("refreshBoxes wellChunk=%d,%d landBoxes=%d buildings=%d",
                    next.startChunkX, next.startChunkZ,
                    next.landBoxesOrEmpty().size(), next.buildingBoxesOrEmpty().size());
        }
        return next;
    }

    private static Record replaceBoxes(long seed, Record rec) {
        if (rec == null || rec.start == null) return rec;
        int[] xz = Reflect.getStructureStartBoxXZ(rec.start);
        if (xz == null) xz = rec.xz;
        int minY = Reflect.getStructureStartMinY(rec.start);
        int maxY = Reflect.getStructureStartMaxY(rec.start);
        if (minY == Integer.MIN_VALUE) minY = rec.minY;
        if (maxY == Integer.MIN_VALUE) maxY = rec.maxY;
        Record next = new Record(rec.start, xz, landBoxesOf(rec.start), buildingBoxesOf(rec.start),
                shrineBoxesOf(rec.start), rec.wellX, rec.wellZ, minY, maxY,
                rec.startChunkX, rec.startChunkZ, true);
        List<Record> list = STARTS.get(seed);
        if (list != null) {
            synchronized (list) {
                for (int i = 0; i < list.size(); i++) {
                    Record existing = list.get(i);
                    if (existing.startChunkX == rec.startChunkX && existing.startChunkZ == rec.startChunkZ) {
                        list.set(i, next);
                        return next;
                    }
                }
            }
        }
        return next;
    }

    private static boolean landOverlapsChunk(Record rec, int chunkMinX, int chunkMaxX,
                                            int chunkMinZ, int chunkMaxZ, int extra) {
        if (rec == null) return false;
        int e = Math.max(0, extra);
        for (int[] box : rec.landBoxesOrEmpty()) {
            if (overlapsXZ(box, chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ, e)) return true;
        }
        return false;
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

    /**
     * Euclidean XZ distance to an AABB: 0 inside, rounded corners outside. Same as flatten hard pad.
     */
    public static double distanceToBoxXZ(int x, int z, int minX, int maxX, int minZ, int maxZ) {
        int dx = 0;
        if (x < minX) dx = minX - x;
        else if (x > maxX) dx = x - maxX;
        int dz = 0;
        if (z < minZ) dz = minZ - z;
        else if (z > maxZ) dz = z - maxZ;
        if (dx == 0 && dz == 0) return 0.0;
        return Math.sqrt((double) dx * dx + (double) dz * dz);
    }

    public static double distanceToBoxXZ(int x, int z, int[] box) {
        if (box == null) return Double.MAX_VALUE;
        return distanceToBoxXZ(x, z, box[0], box[1], box[2], box[3]);
    }

    public static double distanceBetweenAabbXZ(int minX, int maxX, int minZ, int maxZ, int[] box) {
        if (box == null) return Double.MAX_VALUE;
        int dx = 0;
        if (maxX < box[0]) dx = box[0] - maxX;
        else if (minX > box[1]) dx = minX - box[1];
        int dz = 0;
        if (maxZ < box[2]) dz = box[2] - maxZ;
        else if (minZ > box[3]) dz = minZ - box[3];
        if (dx == 0 && dz == 0) return 0.0;
        return Math.sqrt((double) dx * dx + (double) dz * dz);
    }

    /** Flatten hard pad: land {@code villageComponentPad}, shrine {@code smallShrinePad}. */
    public static boolean inVillagePadXZ(int x, int z, Record rec) {
        if (rec == null) return false;
        int landPad = Math.max(0, ArcanaQuestTweaksConfig.RtgModuleConfig.surface.villageComponentPad);
        int shrinePad = Math.max(0, ArcanaQuestTweaksConfig.RtgModuleConfig.surface.smallShrinePad);
        for (int[] box : rec.landBoxesOrEmpty()) {
            if (distanceToBoxXZ(x, z, box) <= landPad) return true;
        }
        for (int[] box : rec.shrineBoxesOrEmpty()) {
            if (distanceToBoxXZ(x, z, box) <= shrinePad) return true;
        }
        return false;
    }

    /** Detection / saved pad pieces: AABB expand by component pad + Hermite falloff. */
    public static boolean inVillagePlateXZ(int x, int z, Record rec) {
        if (rec == null) return false;
        int landR = detectionLandRadius();
        int shrineR = detectionShrineRadius();
        for (int[] box : rec.landBoxesOrEmpty()) {
            if (inExpandedBoxXZ(x, z, box, landR)) return true;
        }
        for (int[] box : rec.shrineBoxesOrEmpty()) {
            if (inExpandedBoxXZ(x, z, box, shrineR)) return true;
        }
        return false;
    }

    public static boolean aabbOverlapsVillagePad(int minX, int maxX, int minZ, int maxZ, Record rec) {
        if (rec == null) return false;
        int landR = detectionLandRadius();
        int shrineR = detectionShrineRadius();
        for (int[] box : rec.landBoxesOrEmpty()) {
            if (distanceBetweenAabbXZ(minX, maxX, minZ, maxZ, box) <= landR) return true;
        }
        for (int[] box : rec.shrineBoxesOrEmpty()) {
            if (distanceBetweenAabbXZ(minX, maxX, minZ, maxZ, box) <= shrineR) return true;
        }
        return false;
    }

    private static int detectionLandRadius() {
        return Math.max(0, ArcanaQuestTweaksConfig.RtgModuleConfig.surface.villageComponentPad)
                + Math.max(0, ArcanaQuestTweaksConfig.RtgModuleConfig.surface.villageEdgeFalloff);
    }

    private static int detectionShrineRadius() {
        return Math.max(0, ArcanaQuestTweaksConfig.RtgModuleConfig.surface.smallShrinePad)
                + Math.max(0, ArcanaQuestTweaksConfig.RtgModuleConfig.surface.villageEdgeFalloff);
    }

    private static boolean inExpandedBoxXZ(int x, int z, int[] box, int r) {
        if (box == null) return false;
        int pad = Math.max(0, r);
        return x >= box[0] - pad && x <= box[1] + pad && z >= box[2] - pad && z <= box[3] + pad;
    }

    public static Object startAt(World world, Object mapGen, int x, int y, int z) {
        if (!ArcanaQuestTweaksConfig.RtgModuleConfig.surface.enableVillageBoxDetection) return null;
        if (world == null) return null;
        ensureStarts(world, mapGen);
        if (mapGen != null) {
            rememberNearby(world, mapGen, x >> 4, z >> 4);
        }
        int heightAbove = Math.max(0, ArcanaQuestTweaksConfig.RtgModuleConfig.surface.villageBoxHeight);
        long seed = Reflect.getSeed(world);
        for (Record rec : starts(seed)) {
            if (rec.start == null) continue;
            Record live = maybeRefreshLandBoxes(seed, rec);
            float plate = resolvePlateOrSample(world, live);
            if (Float.isNaN(plate)) continue;
            if (!yInVillageVolume(y, plate, heightAbove, live)) continue;
            if (!inVillagePlateXZ(x, z, live)) continue;
            return live.start;
        }
        return null;
    }

    public static float resolvePlateOrSample(World world, Record rec) {
        float cached = resolvePlate(world, rec);
        if (!Float.isNaN(cached)) return cached;
        if (world == null || rec == null) return Float.NaN;
        int minWell = VillageLandHelper.minWellHeight();
        float wellHeight = VillageLandHelper.sampleNoise(world, rec.wellX, rec.wellZ);
        long seed = Reflect.getSeed(world);
        if (VillageLandHelper.isNeverRaiseAt(world, rec.wellX, rec.wellZ)
                || !VillageLandHelper.isUsableHeight(wellHeight)) {
            try {
                if (VillageLandHelper.isSwampLikeForRaise(
                        Reflect.getBiome(world.getBiomeProvider(), rec.wellX, rec.wellZ))) {
                    put(seed, rec, (float) minWell);
                    return minWell;
                }
            } catch (Throwable ignored) {}
            try {
                int y = world.getHeight(rec.wellX, rec.wellZ);
                if (y > 0) {
                    put(seed, rec, (float) y);
                    return y;
                }
            } catch (Throwable ignored) {}
            return Float.NaN;
        }
        float target = Math.max(wellHeight, minWell);
        put(seed, rec, target);
        return target;
    }

    /**
     * Append non-placing pad children (one per land/shrine box) and write {@code Village.dat}.
     * Real houses/paths/RC stay as their own children. Idempotent.
     */
    public static void stampDetectionPieces(World world, Record rec, Object mapGen) {
        if (!ArcanaQuestTweaksConfig.RtgModuleConfig.surface.enableVillageBoxDetection) return;
        if (world == null || rec == null || rec.start == null) return;
        for (Object piece : Reflect.getStructureStartComponents(rec.start)) {
            if (VillageLandHelper.isVillagePlatePad(piece)) return;
        }
        float plate = resolvePlateOrSample(world, rec);
        if (Float.isNaN(plate)) return;
        StructureVillagePieces.Start well = villageWellStart(rec.start);
        if (well == null) return;
        int padY = Math.round(plate);
        int floor = wellFloorY(rec, padY);
        int maxY = padY + Math.max(0, ArcanaQuestTweaksConfig.RtgModuleConfig.surface.villageBoxHeight);
        int landR = detectionLandRadius();
        int shrineR = detectionShrineRadius();
        List<int[]> shrines = rec.shrineBoxesOrEmpty();
        boolean any = false;
        for (int[] box : rec.landBoxesOrEmpty()) {
            if (containsXZBox(shrines, box)) continue;
            any |= addPadPiece(rec.start, well, box, landR, floor, maxY);
        }
        for (int[] box : shrines) {
            any |= addPadPiece(rec.start, well, box, shrineR, floor, maxY);
        }
        if (!any) return;
        Reflect.updateStructureStartBoundingBox(rec.start);
        Reflect.saveMapGenStructureStart(mapGen, world, rec.start);
        if (VillageDebug.once("stampPlate:" + wellKey(Reflect.getSeed(world), rec))) {
            VillageDebug.log("stamp village plate well=%d,%d floor=%d maxY=%d landR=%d",
                    rec.wellX, rec.wellZ, floor, maxY, landR);
        }
    }

    private static StructureVillagePieces.Start villageWellStart(Object start) {
        for (Object piece : Reflect.getStructureStartComponents(start)) {
            if (piece instanceof StructureVillagePieces.Start) {
                return (StructureVillagePieces.Start) piece;
            }
        }
        return null;
    }

    private static boolean addPadPiece(Object start, StructureVillagePieces.Start well,
                                       int[] box, int radius, int minY, int maxY) {
        if (box == null) return false;
        int r = Math.max(0, radius);
        StructureBoundingBox aabb = new StructureBoundingBox(
                box[0] - r, minY, box[2] - r,
                box[1] + r, maxY, box[3] + r);
        VillagePieceVillagePlate pad = new VillagePieceVillagePlate(well, aabb);
        return Reflect.addStructureStartComponent(start, pad);
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
    public static float resolvePlate(World world, Record rec) {
        long seed = world != null ? Reflect.getSeed(world) : 0L;
        Float cached = get(seed, rec);
        return cached != null ? cached : Float.NaN;
    }

    public static float resolvePlate(World world, int[] box) {
        if (box == null) return Float.NaN;
        long seed = world != null ? Reflect.getSeed(world) : 0L;
        for (Record rec : starts(seed)) {
            if (rec.xz != null && key(seed, rec.xz).equals(key(seed, box))) {
                return resolvePlate(world, rec);
            }
        }
        Float cached = HEIGHTS.get(key(seed, box));
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
        long seed = world != null ? Reflect.getSeed(world) : 0L;
        if (box != null) {
            for (Record rec : starts(seed)) {
                if (rec.xz != null && key(seed, rec.xz).equals(key(seed, box))) {
                    put(seed, rec, height);
                    break;
                }
            }
        }
        return height;
    }

    public static List<int[]> landBoxesOf(Object start) {
        VillageLandHelper.pushColumnLandscapeCache();
        try {
            List<int[]> out = new ArrayList<>();
            for (Object piece : Reflect.getStructureStartComponents(start)) {
                if (VillageLandHelper.isVillagePlatePad(piece)) continue;
                int[] box = Reflect.getStructureComponentBoxXZ(piece);
                if (box == null) continue;
                if (VillageLandHelper.isVillageRoad(piece) && VillageLandHelper.isAabbFullyFlooded(start, piece)) {
                    continue;
                }
                out.add(box);
            }
            return out;
        } finally {
            VillageLandHelper.popColumnLandscapeCache();
        }
    }

    /**
     * Houses, RC, and the well. Roads and docks are excluded so swamp raise stays rounded around buildings.
     */
    public static List<int[]> buildingBoxesOf(Object start) {
        List<int[]> out = new ArrayList<>();
        for (Object piece : Reflect.getStructureStartComponents(start)) {
            if (VillageLandHelper.isVillagePlatePad(piece)) continue;
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
