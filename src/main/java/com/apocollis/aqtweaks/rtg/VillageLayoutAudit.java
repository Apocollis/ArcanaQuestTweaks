package com.apocollis.aqtweaks.rtg;

import com.apocollis.aqtweaks.perf.StageTimer;
import net.minecraft.world.World;
import rtg.world.gen.ChunkGeneratorRTG;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Per-village counts for the layout-tuning audit. Placement is unchanged.
 * A line is written once per village, and only when village debug or the perf timers are on.
 */
public final class VillageLayoutAudit {

    private static final int KINDS = 4;
    private static final ThreadLocal<IdentityHashMap<Object, Counts>> ACTIVE =
            ThreadLocal.withInitial(IdentityHashMap::new);
    private static final ThreadLocal<IdentityHashMap<Object, Piece>> HELD =
            ThreadLocal.withInitial(IdentityHashMap::new);

    private VillageLayoutAudit() {}

    public static void acceptRoad(Object start, Object piece) {
        accept(start, piece, true);
    }

    public static void acceptHouse(Object start, Object piece) {
        accept(start, piece, false);
    }

    /** A road returned while a path retry is on the stack. Removed attempts are undone by {@link #drop}. */
    public static void stackRoad(Object start, Object piece) {
        stack(start, piece, true);
    }

    public static void stackHouse(Object start, Object piece) {
        stack(start, piece, false);
    }

    public static void drop(Object piece) {
        if (piece == null || !recording()) return;
        Piece held = HELD.get().remove(piece);
        if (held == null) return;
        Counts counts = ACTIVE.get().get(held.start);
        if (counts == null) return;
        if (held.road) {
            if (counts.roadsPlaced > 0) counts.roadsPlaced--;
            if (held.retried && counts.roadsRetried > 0) counts.roadsRetried--;
            undoHit(counts.roadHits, held.hit);
        } else {
            if (counts.housesPlaced > 0) counts.housesPlaced--;
            if (held.retried && counts.housesRetried > 0) counts.housesRetried--;
            undoHit(counts.houseHits, held.hit);
        }
    }

    public static void retryRoad(Object start) {
        if (start == null || !recording()) return;
        counts(start).roadsRetried++;
    }

    public static void retryHouse(Object start) {
        if (start == null || !recording()) return;
        counts(start).housesRetried++;
    }

    public static void hitRoad(Object start, Object piece, int kind) {
        if (start == null || !recording()) return;
        kind = hit(counts(start).roadHits, kind);
        markHit(piece, kind);
    }

    public static void hitHouse(Object start, Object piece, int kind) {
        if (start == null || !recording()) return;
        kind = hit(counts(start).houseHits, kind);
        markHit(piece, kind);
    }

    public static void missRoad(Object start) {
        if (start == null || !recording()) return;
        counts(start).roadsMiss++;
    }

    public static void missHouse(Object start) {
        if (start == null || !recording()) return;
        counts(start).housesMiss++;
    }

    /**
     * One line per village touched during this layout. Skips the landscape sample when both outputs are off.
     */
    public static void flush(World world) {
        IdentityHashMap<Object, Counts> active = ACTIVE.get();
        HELD.get().clear();
        if (active.isEmpty()) return;
        if (!recording()) {
            active.clear();
            return;
        }
        int dim = world != null && world.provider != null ? world.provider.getDimension() : Integer.MIN_VALUE;
        for (Map.Entry<Object, Counts> entry : active.entrySet()) {
            Object start = entry.getKey();
            Counts counts = entry.getValue();
            int[] box = StructureAccess.getStructureComponentBoxXZ(start);
            String key = box == null
                    ? dim + ":id:" + System.identityHashCode(start)
                    : dim + ":" + box[0] + ":" + box[2];
            if (counts.reportedKey != null) continue;
            if (!Reported.add(key)) {
                counts.reportedKey = key;
                continue;
            }
            counts.reportedKey = key;
            String line = describe(world, start, counts, box);
            if (VillageDebug.enabled()) {
                VillageDebug.log("%s", line);
            }
            if (StageTimer.enabled()) {
                StageTimer.recordNs("village.layoutAudit", 0L, line);
            }
        }
        active.clear();
    }

    private static void accept(Object start, Object piece, boolean road) {
        if (start == null || !recording()) return;
        if (piece != null && HELD.get().containsKey(piece)) return;
        if (road) counts(start).roadsPlaced++;
        else counts(start).housesPlaced++;
        if (piece != null) HELD.get().put(piece, new Piece(start, road));
    }

    private static void stack(Object start, Object piece, boolean road) {
        if (start == null || piece == null || !recording()) return;
        if (HELD.get().containsKey(piece)) return;
        HELD.get().put(piece, new Piece(start, road));
        if (road) counts(start).roadsPlaced++;
        else counts(start).housesPlaced++;
    }

    private static int hit(int[] hits, int kind) {
        if (kind < 0 || kind >= hits.length) kind = 0;
        hits[kind]++;
        return kind;
    }

    private static void markHit(Object piece, int kind) {
        if (piece == null) return;
        Piece held = HELD.get().get(piece);
        if (held == null) return;
        held.hit = kind;
        held.retried = true;
    }

    private static void undoHit(int[] hits, int kind) {
        if (kind < 0 || kind >= hits.length) return;
        if (hits[kind] > 0) hits[kind]--;
    }

    private static Counts counts(Object start) {
        IdentityHashMap<Object, Counts> map = ACTIVE.get();
        Counts counts = map.get(start);
        if (counts == null) {
            counts = new Counts();
            map.put(start, counts);
        }
        return counts;
    }

    private static boolean recording() {
        return VillageDebug.enabled() || StageTimer.enabled();
    }

    private static String describe(World world, Object start, Counts counts, int[] box) {
        int roads = 0;
        int houses = 0;
        int pieces = 0;
        for (Object piece : StructureAccess.getStructureStartComponents(start)) {
            pieces++;
            if (VillageLandHelper.isVillageRoad(piece)) roads++;
            else if (!VillageLandHelper.isVillageWellOrStart(piece) && !VillageLandHelper.isVillagePlatePad(piece)) houses++;
        }
        int area = 0;
        int wellX = 0;
        int wellZ = 0;
        if (box != null) {
            area = Math.max(0, box[1] - box[0] + 1) * Math.max(0, box[3] - box[2] + 1);
            wellX = (box[0] + box[1]) >> 1;
            wellZ = (box[2] + box[3]) >> 1;
        }
        int dry = 0;
        int sampled = 0;
        int hMin = Integer.MAX_VALUE;
        int hMax = Integer.MIN_VALUE;
        if (world != null && box != null) {
            ChunkGeneratorRTG gen = VillageLandHelper.currentGenerator();
            for (int ox = -32; ox <= 32; ox += 16) {
                for (int oz = -32; oz <= 32; oz += 16) {
                    int x = wellX + ox;
                    int z = wellZ + oz;
                    sampled++;
                    if (!VillageLandHelper.isWaterAt(start, x, z)) dry++;
                    float height = gen != null
                            ? VillageLandHelper.sampleNoise(gen, world.getBiomeProvider(), x, z)
                            : VillageLandHelper.sampleNoise(world, x, z);
                    if (!VillageLandHelper.isUsableHeight(height)) continue;
                    int y = Math.round(height);
                    if (y < hMin) hMin = y;
                    if (y > hMax) hMax = y;
                }
            }
        }
        if (hMin == Integer.MAX_VALUE) {
            hMin = 0;
            hMax = 0;
        }
        return "village layout well=" + wellX + "," + wellZ
                + " roads placed=" + counts.roadsPlaced
                + " retried=" + counts.roadsRetried
                + " miss=" + counts.roadsMiss
                + " hits=" + hits(counts.roadHits)
                + " houses placed=" + counts.housesPlaced
                + " retried=" + counts.housesRetried
                + " miss=" + counts.housesMiss
                + " hits=" + hits(counts.houseHits)
                + " pieces=" + pieces
                + " pieceRoads=" + roads
                + " pieceHouses=" + houses
                + " footprint=" + area
                + " cap=112"
                + " dry=" + dry + "/" + sampled
                + " h=" + hMin + ".." + hMax;
    }

    private static String hits(int[] hits) {
        return "back:" + hits[VillageLandHelper.SLOT_BACK]
                + ",lat:" + hits[VillageLandHelper.SLOT_LATERAL]
                + ",well:" + hits[VillageLandHelper.SLOT_TOWARD_WELL]
                + ",spiral:" + hits[VillageLandHelper.SLOT_SPIRAL];
    }

    private static final class Counts {
        int roadsPlaced;
        int roadsRetried;
        int roadsMiss;
        final int[] roadHits = new int[KINDS];
        int housesPlaced;
        int housesRetried;
        int housesMiss;
        final int[] houseHits = new int[KINDS];
        String reportedKey;
    }

    private static final class Piece {
        final Object start;
        final boolean road;
        int hit = -1;
        boolean retried;

        Piece(Object start, boolean road) {
            this.start = start;
            this.road = road;
        }
    }

    /** Villages already printed this session, keyed by dimension and the start box min corner. */
    private static final class Reported {
        private static final java.util.Set<String> KEYS = java.util.concurrent.ConcurrentHashMap.newKeySet();

        static boolean add(String key) {
            return KEYS.add(key);
        }
    }

}
