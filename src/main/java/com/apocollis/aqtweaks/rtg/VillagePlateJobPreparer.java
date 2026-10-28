package com.apocollis.aqtweaks.rtg;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.chunksteward.api.JobPreparer;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.structure.MapGenVillage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import rtg.world.gen.ChunkGeneratorRTG;

import java.util.Arrays;
import java.util.List;

/**
 * Chunk Steward job preparer for the village plate (flatten and seal) passes. Both run on the server thread inside
 * Chunk Steward's tick-start ranking, with its pipeline lock held:
 * <ul>
 * <li>{@link #prepare}: the village layout that {@code MixinChunkGeneratorRTGVillage#aqtweaks$registerVillages} runs
 * for a server-thread chunk, then the same {@link VillagePlateSnapshot} the inline flatten and seal hooks build
 * (records near the chunk, plate heights resolved). The worker reads that snapshot instead of the live registry.</li>
 * <li>{@link #isStillValid}: read-only. False only when the set of villages overlapping the chunk changed since
 * {@code prepare}; the worker chunk is then discarded and the server thread generates it inline.</li>
 * </ul>
 *
 * <p>Neither method calls {@code World.getChunk}, {@code provideChunk}, {@code generateChunk} or any
 * {@code ChunkStewardAPI} method ({@link PipelineContext#active()} answers false while {@code prepare} runs, so code
 * the layout reaches does not ask the API). The layout uses the server's stashed generators, which is why a worker
 * generator must never be stashed (see {@link PipelineContext}).
 *
 * <p>Loaded only after {@link PipelineContext} read API version 3: it implements a v3 type.
 */
public final class VillagePlateJobPreparer implements JobPreparer {

    private static final Logger LOGGER = LogManager.getLogger("AQTweaks-Village");
    private static final ThreadLocal<ChunkPrimer> DUMMY_PRIMER = ThreadLocal.withInitial(ChunkPrimer::new);
    private static volatile boolean failureLogged;

    @Override
    public Object prepare(World world, int chunkX, int chunkZ) {
        if (world == null) return null;
        long seed = world.getSeed();
        if (!ArcanaQuestTweaksConfig.RtgModuleConfig.surface.enableVillageSmoothing) {
            return VillagePlateSnapshot.empty(seed, new long[0], VillagePlateSnapshot.NOT_TRACKED);
        }
        PipelineContext.beginPrepare();
        try {
            if (!StructureAccess.isMapFeaturesEnabled(world, true)) {
                return VillagePlateSnapshot.empty(seed, new long[0], VillagePlateSnapshot.NOT_TRACKED);
            }
            MapGenVillage village = VillageLandHelper.stashedVillage(world);
            ChunkGeneratorRTG rtg = VillageLandHelper.stashedRtg(world);
            if (village == null || rtg == null) {
                return VillagePlateSnapshot.unavailable(seed);
            }
            VillageLandHelper.pushGenerator(rtg);
            try {
                VillageLandHelper.layoutVillageGrid(village, world, chunkX, chunkZ, DUMMY_PRIMER.get());
            } finally {
                VillageLandHelper.popGenerator();
            }

            int flattenReach = VillagePlateSnapshots.flattenReach();
            int sealReach = VillagePlateSnapshots.sealReach();
            VillageLandHelper.pushGenerator(rtg);
            try {
                List<VillagePlate.Record> flat = VillagePlateSnapshots.villageHits(
                        world, village, chunkX, chunkZ, flattenReach, true);
                float[] flatY = VillagePlateSnapshots.plateHeights(world, rtg, flat);
                List<VillagePlate.Record> seal = VillagePlateSnapshots.villageHits(
                        world, village, chunkX, chunkZ, sealReach, false);
                float[] sealY = VillagePlateSnapshots.plateHeights(world, rtg, seal);
                int keyReach = Math.max(flattenReach, sealReach);
                long[] keys = VillagePlateSnapshots.overlappingKeys(seed, chunkX, chunkZ, keyReach);
                if (flat.isEmpty() && seal.isEmpty()) {
                    return VillagePlateSnapshot.empty(seed, keys, keyReach);
                }
                return VillagePlateSnapshot.ofPipeline(seed, flat, flatY, seal, sealY, keys, keyReach);
            } finally {
                VillageLandHelper.popGenerator();
            }
        } catch (Throwable t) {
            if (!failureLogged) {
                failureLogged = true;
                LOGGER.error("village prepare failed at chunk {},{}; chunks near villages generate inline", chunkX, chunkZ, t);
            }
            return VillagePlateSnapshot.unavailable(seed);
        } finally {
            PipelineContext.endPrepare();
        }
    }

    @Override
    public boolean isStillValid(Object context, World world, int chunkX, int chunkZ) {
        if (!(context instanceof VillagePlateSnapshot) || world == null) return true;
        VillagePlateSnapshot snapshot = (VillagePlateSnapshot) context;
        if (snapshot.unavailable) return false;
        if (snapshot.keyReach == VillagePlateSnapshot.NOT_TRACKED) return true;
        long[] live = VillagePlateSnapshots.overlappingKeys(snapshot.seed, chunkX, chunkZ, snapshot.keyReach);
        return Arrays.equals(live, snapshot.chunkKeys);
    }
}
