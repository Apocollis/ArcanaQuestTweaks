package com.apocollis.aqtweaks.rtg;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.gen.structure.MapGenVillage;
import rtg.world.gen.ChunkGeneratorRTG;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Builds {@link VillagePlateSnapshot}s. The flatten and seal hooks in {@code MixinChunkGeneratorRTGVillage} call
 * these on the server thread (unchanged behaviour); {@link VillagePlateJobPreparer} calls the same code from
 * {@code prepare}, so a pipeline snapshot is exactly the input the inline path would have built.
 *
 * <p>These read and write the live registry ({@link VillagePlate}), so they are server-thread only. A terrain
 * worker never calls them.
 */
public final class VillagePlateSnapshots {

    private VillagePlateSnapshots() {}

    /**
     * The village snapshot of the job this worker is generating, or null when there is none to use: no context, no
     * {@link VillagePlateSnapshot} among the preparer results, or one marked unavailable. Found with
     * {@code instanceof}, since the array holds one entry per registered preparer in registration order.
     */
    public static VillagePlateSnapshot jobSnapshot() {
        Object[] contexts = PipelineContext.jobContext();
        if (contexts == null) return null;
        for (Object context : contexts) {
            if (context instanceof VillagePlateSnapshot) {
                VillagePlateSnapshot snapshot = (VillagePlateSnapshot) context;
                return snapshot.unavailable ? null : snapshot;
            }
        }
        return null;
    }

    /** Reach of the flatten pass: village component pad plus the edge falloff. Same formula as the flatten hook. */
    public static int flattenReach() {
        int falloff = Math.max(0, ArcanaQuestTweaksConfig.RtgModuleConfig.surface.villageEdgeFalloff);
        int componentPad = Math.max(0, ArcanaQuestTweaksConfig.RtgModuleConfig.surface.villageComponentPad);
        return componentPad + falloff;
    }

    /** Reach of the seal pass. Same formula as the seal hook. */
    public static int sealReach() {
        int componentPad = Math.max(0, ArcanaQuestTweaksConfig.RtgModuleConfig.surface.villageComponentPad);
        int shrinePad = Math.max(0, ArcanaQuestTweaksConfig.RtgModuleConfig.surface.smallShrinePad);
        return Math.max(componentPad, shrinePad);
    }

    /**
     * The one input of the flatten and seal passes: the villages near this chunk and their plate heights, copied
     * once (see {@link VillagePlateSnapshot}). {@code recover} is the flatten behaviour (rebuild and recover records),
     * seal reads the registry as it is. With {@code Verify Village Snapshot} on, the copy is compared with the live
     * records and a fresh plate-height resolve.
     */
    public static VillagePlateSnapshot snapshot(World world, MapGenVillage villageGenerator, ChunkGeneratorRTG rtg,
                                                int cx, int cz, int reach, boolean recover, String where) {
        List<VillagePlate.Record> live = villageHits(world, villageGenerator, cx, cz, reach, recover);
        float[] plateY = plateHeights(world, rtg, live);
        VillagePlateSnapshot snapshot = VillagePlateSnapshot.of(world.getSeed(), live, plateY);
        if (ArcanaQuestTweaksConfig.RtgModuleConfig.surface.verifyVillageSnapshot) {
            float[] fresh = plateHeights(world, rtg, live);
            VillagePlateSnapshot.verify(snapshot, live, fresh, where);
        }
        return snapshot;
    }

    /** Plate height of each record, resolved in list order with {@code rtg}. */
    public static float[] plateHeights(World world, ChunkGeneratorRTG rtg, List<VillagePlate.Record> records) {
        float[] plateY = new float[records.size()];
        for (int i = 0; i < plateY.length; i++) {
            plateY[i] = getOrComputePlateHeight(world, rtg, records.get(i));
        }
        return plateY;
    }

    public static List<VillagePlate.Record> villageHits(World world, MapGenVillage villageGenerator,
                                                        int cx, int cz, int reach, boolean recover) {
        int startX = cx * 16;
        int startZ = cz * 16;
        int chunkMaxX = startX + 15;
        int chunkMaxZ = startZ + 15;
        long seed = world.getSeed();
        List<VillagePlate.Record> hits = VillagePlate.overlappingRecords(
                seed, startX, chunkMaxX, startZ, chunkMaxZ, reach);
        if (!hits.isEmpty()) {
            return recover ? VillagePlate.refreshUnlocked(seed, hits) : hits;
        }
        if (!recover) return Collections.emptyList();
        if (VillagePlate.isEmpty(seed)) {
            VillagePlate.ensureStarts(world, villageGenerator);
            hits = VillagePlate.overlappingRecords(seed, startX, chunkMaxX, startZ, chunkMaxZ, reach);
            if (!hits.isEmpty()) return hits;
        }
        VillagePlate.rememberNearby(world, villageGenerator, cx, cz);
        hits = VillagePlate.overlappingRecords(seed, startX, chunkMaxX, startZ, chunkMaxZ, reach);
        if (!hits.isEmpty()) return hits;
        return VillagePlate.mergeStartAabbHits(world, hits, startX, chunkMaxX, startZ, chunkMaxZ, reach);
    }

    public static float getOrComputePlateHeight(World world, ChunkGeneratorRTG rtg, VillagePlate.Record rec) {
        long seed = world != null ? world.getSeed() : 0L;
        Float cached = VillagePlate.get(seed, rec);
        if (cached != null) return cached;

        BiomeProvider biomeProvider = StructureAccess.getBiomeProvider(world);
        if (biomeProvider == null) {
            return Float.NaN;
        }
        Biome wellBiome = StructureAccess.getBiome(biomeProvider, rec.wellX, rec.wellZ);
        boolean swampWell = VillageLandHelper.isSwampLikeForRaise(wellBiome);
        boolean neverRaiseWell = VillageLandHelper.isNeverRaiseAt(world, rec.wellX, rec.wellZ);
        int minWell = VillageLandHelper.minWellHeight();

        float wellHeight = VillageLandHelper.sampleNoise(rtg, biomeProvider, rec.wellX, rec.wellZ);
        if (neverRaiseWell || !VillageLandHelper.isUsableHeight(wellHeight)) {
            if (swampWell) {
                VillagePlate.put(seed, rec, (float) minWell);
                if (VillageDebug.once("plateSample:" + VillagePlate.wellKey(seed, rec))) {
                    VillageDebug.log("plateSample well=%d,%d biome=%s raw=none target=%.1f source=swamp64 fallback=yes",
                            rec.wellX, rec.wellZ, VillageLandHelper.biomeId(wellBiome), (float) minWell);
                }
                return minWell;
            }
            if (VillageDebug.once("plateSample:" + VillagePlate.wellKey(seed, rec))) {
                VillageDebug.log("plateSample well=%d,%d biome=%s raw=none target=skip reason=%s",
                        rec.wellX, rec.wellZ, VillageLandHelper.biomeId(wellBiome),
                        neverRaiseWell ? "never-raise" : "unusable");
            }
            return Float.NaN;
        }

        float target = Math.max(wellHeight, minWell);
        boolean fallback = wellHeight < minWell;
        VillagePlate.put(seed, rec, target);
        if (VillageDebug.once("plateSample:" + VillagePlate.wellKey(seed, rec))) {
            VillageDebug.log("plateSample well=%d,%d biome=%s raw=%.1f target=%.1f source=well fallback=%s",
                    rec.wellX, rec.wellZ, VillageLandHelper.biomeId(wellBiome),
                    wellHeight, target, fallback ? "yes" : "no");
        }
        return target;
    }

    /**
     * Identity of every village that overlaps the chunk by land box or by start box, as sorted start-chunk keys.
     * Read-only: no layout, no refresh, no recovery. {@code isStillValid} compares two of these.
     */
    public static long[] overlappingKeys(long seed, int cx, int cz, int reach) {
        int startX = cx * 16;
        int startZ = cz * 16;
        int chunkMaxX = startX + 15;
        int chunkMaxZ = startZ + 15;
        List<Long> keys = new ArrayList<>();
        for (VillagePlate.Record rec : VillagePlate.overlappingRecords(seed, startX, chunkMaxX, startZ, chunkMaxZ, reach)) {
            keys.add(ChunkPos.asLong(rec.startChunkX, rec.startChunkZ));
        }
        for (VillagePlate.Record rec : VillagePlate.overlappingStartAabb(seed, startX, chunkMaxX, startZ, chunkMaxZ, reach)) {
            keys.add(ChunkPos.asLong(rec.startChunkX, rec.startChunkZ));
        }
        long[] out = new long[keys.size()];
        for (int i = 0; i < out.length; i++) out[i] = keys.get(i);
        Arrays.sort(out);
        int n = 0;
        for (int i = 0; i < out.length; i++) {
            if (i == 0 || out[i] != out[i - 1]) out[n++] = out[i];
        }
        return n == out.length ? out : Arrays.copyOf(out, n);
    }
}
