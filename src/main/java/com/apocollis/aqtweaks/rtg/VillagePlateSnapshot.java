package com.apocollis.aqtweaks.rtg;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An immutable value copy of the {@link VillagePlate.Record}s one chunk's flatten or seal pass reads, plus the plate
 * height of each, taken at one moment on the thread that builds it.
 *
 * <p>The flatten and seal hooks read this instead of the live registry, so what they use cannot change under them
 * and, later, a Chunk Steward worker can run them without touching the registry (plan
 * {@code 20261026-0008-village-plate-snapshot-for-chunksteward-pipeline.md}). The hooks have one code path: the
 * synchronous path builds the same snapshot inline and calls the same body.
 *
 * <p>Holds no {@code StructureStart}, no {@code World} and no generator. The box arrays are cloned and must not be
 * mutated by readers (none of them do). The lazy well-Y fields of {@link VillagePlate.Record} are not part of this
 * snapshot: flatten and seal never read them.
 *
 * <p>Not covered, and still live reads that a worker path must resolve separately (audit, appendix of the plan):
 * chunk-dependent biome reads ({@code VillageLandHelper.isOceanOrRiverBiomeSources} reads {@code world.getBiome} of
 * loaded chunks), the biome provider, and RTG landscape sampling.
 */
public final class VillagePlateSnapshot {

    private static final Logger LOGGER = LogManager.getLogger("AQTweaks-Village");
    private static volatile boolean mismatchLogged;

    /** One village's data as a flatten or seal pass uses it. */
    public static final class Rec {
        public final int[] xz;
        public final int wellX;
        public final int wellZ;
        public final int startChunkX;
        public final int startChunkZ;
        /** Plate height resolved when the snapshot was built; NaN means "skip this village". */
        public final float plateY;
        private final List<int[]> landBoxes;
        private final List<int[]> buildingBoxes;
        private final List<int[]> shrineBoxes;

        private Rec(VillagePlate.Record rec, float plateY) {
            this.xz = rec.xz == null ? null : rec.xz.clone();
            this.wellX = rec.wellX;
            this.wellZ = rec.wellZ;
            this.startChunkX = rec.startChunkX;
            this.startChunkZ = rec.startChunkZ;
            this.plateY = plateY;
            this.landBoxes = copy(rec.landBoxesOrEmpty());
            this.buildingBoxes = copy(rec.buildingBoxesOrEmpty());
            this.shrineBoxes = copy(rec.shrineBoxesOrEmpty());
        }

        public List<int[]> landBoxesOrEmpty() {
            return landBoxes;
        }

        public List<int[]> buildingBoxesOrEmpty() {
            return buildingBoxes;
        }

        public List<int[]> shrineBoxesOrEmpty() {
            return shrineBoxes;
        }

        /** Same key {@link VillagePlate#wellKey(long, VillagePlate.Record)} gives for the live record. */
        public String wellKey(long seed) {
            return VillagePlate.wellKey(seed, startChunkX, startChunkZ);
        }
    }

    public final long seed;
    /** The villages the flatten pass reads (the inline flatten input: recovered, flatten reach). */
    public final List<Rec> records;
    /** The villages the seal pass reads (the inline seal input: registry as it is, seal reach). Empty when inline. */
    public final List<Rec> sealRecords;
    /**
     * Pipeline snapshot only: identity (start chunk) of every village overlapping the chunk when it was taken, sorted.
     * {@link VillagePlateJobPreparer#isStillValid} compares it with the live set. Empty for the inline snapshot.
     */
    public final long[] chunkKeys;
    /** {@link #keyReach} of a snapshot with no villages to track (smoothing off, map features off): always valid. */
    public static final int NOT_TRACKED = -1;
    /** The reach {@link #chunkKeys} was taken with, or {@link #NOT_TRACKED}. */
    public final int keyReach;
    /** Pipeline snapshot only: prepare failed or had no generator, so a worker chunk made with it must be discarded. */
    public final boolean unavailable;

    private VillagePlateSnapshot(long seed, List<Rec> records, List<Rec> sealRecords, long[] chunkKeys,
                                 int keyReach, boolean unavailable) {
        this.seed = seed;
        this.records = Collections.unmodifiableList(records);
        this.sealRecords = Collections.unmodifiableList(sealRecords);
        this.chunkKeys = chunkKeys;
        this.keyReach = keyReach;
        this.unavailable = unavailable;
    }

    /** No village near the chunk: both passes write nothing. */
    public static VillagePlateSnapshot empty(long seed, long[] chunkKeys, int keyReach) {
        return new VillagePlateSnapshot(seed, new ArrayList<>(), new ArrayList<>(), chunkKeys, keyReach, false);
    }

    /** Prepare could not take a trustworthy snapshot: a chunk generated on a worker with it is discarded. */
    public static VillagePlateSnapshot unavailable(long seed) {
        return new VillagePlateSnapshot(seed, new ArrayList<>(), new ArrayList<>(), new long[0], 0, true);
    }

    /**
     * The pipeline form: flatten and seal inputs taken once on the server thread, with the identity set that
     * {@code isStillValid} checks. Lists and plate arrays are parallel, as in {@link #of(long, List, float[])}.
     */
    public static VillagePlateSnapshot ofPipeline(long seed, List<VillagePlate.Record> flatHits, float[] flatPlateY,
                                                  List<VillagePlate.Record> sealHits, float[] sealPlateY,
                                                  long[] chunkKeys, int keyReach) {
        return new VillagePlateSnapshot(seed, copyAll(flatHits, flatPlateY), copyAll(sealHits, sealPlateY),
                chunkKeys, keyReach, false);
    }

    private static List<Rec> copyAll(List<VillagePlate.Record> hits, float[] plateY) {
        List<Rec> out = new ArrayList<>(hits.size());
        for (int i = 0; i < hits.size(); i++) {
            out.add(new Rec(hits.get(i), plateY[i]));
        }
        return out;
    }

    /**
     * Copies {@code hits}. {@code plateY[i]} is the plate height of {@code hits.get(i)}, resolved by the caller with
     * its own generator ({@code getOrComputePlateHeight}) in list order, the order the passes resolved them in before
     * the snapshot existed.
     */
    public static VillagePlateSnapshot of(long seed, List<VillagePlate.Record> hits, float[] plateY) {
        return new VillagePlateSnapshot(seed, copyAll(hits, plateY), new ArrayList<>(), new long[0], 0, false);
    }

    private static List<int[]> copy(List<int[]> boxes) {
        if (boxes == null || boxes.isEmpty()) return Collections.emptyList();
        List<int[]> out = new ArrayList<>(boxes.size());
        for (int[] box : boxes) {
            out.add(box == null ? null : box.clone());
        }
        return Collections.unmodifiableList(out);
    }

    /**
     * Dev-only ({@code Verify Village Snapshot}): compares every snapshot record with the live record the passes read
     * before the snapshot existed, and its plate height with a fresh resolve ({@code freshPlateY}), and logs the
     * first difference ({@code [AQ-VERIFY]}). The flatten and seal bodies are the same code on either input, so
     * equal inputs mean equal output.
     */
    public static void verify(VillagePlateSnapshot snap, List<VillagePlate.Record> live,
                              float[] freshPlateY, String where) {
        if (mismatchLogged) return;
        if (snap.records.size() != live.size()) {
            fail(where, "record count " + snap.records.size() + " vs " + live.size());
            return;
        }
        for (int i = 0; i < live.size(); i++) {
            VillagePlate.Record rec = live.get(i);
            Rec copy = snap.records.get(i);
            String why = difference(rec, copy);
            if (why == null) {
                if (Float.floatToIntBits(freshPlateY[i]) != Float.floatToIntBits(copy.plateY)) {
                    why = "plateY " + copy.plateY + " vs " + freshPlateY[i];
                }
            }
            if (why != null) {
                fail(where, "well chunk " + rec.startChunkX + "," + rec.startChunkZ + ": " + why);
                return;
            }
        }
    }

    private static String difference(VillagePlate.Record rec, Rec copy) {
        if (!java.util.Arrays.equals(rec.xz, copy.xz)) return "xz";
        if (rec.wellX != copy.wellX || rec.wellZ != copy.wellZ) return "well";
        if (rec.startChunkX != copy.startChunkX || rec.startChunkZ != copy.startChunkZ) return "start chunk";
        if (!sameBoxes(rec.landBoxesOrEmpty(), copy.landBoxes)) return "land boxes";
        if (!sameBoxes(rec.buildingBoxesOrEmpty(), copy.buildingBoxes)) return "building boxes";
        if (!sameBoxes(rec.shrineBoxesOrEmpty(), copy.shrineBoxes)) return "shrine boxes";
        return null;
    }

    private static boolean sameBoxes(List<int[]> a, List<int[]> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            if (!java.util.Arrays.equals(a.get(i), b.get(i))) return false;
        }
        return true;
    }

    private static void fail(String where, String what) {
        mismatchLogged = true;
        LOGGER.error("[AQ-VERIFY] village snapshot mismatch in {}: {}", where, what);
    }
}
