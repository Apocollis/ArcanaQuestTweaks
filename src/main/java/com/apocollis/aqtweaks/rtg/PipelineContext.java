package com.apocollis.aqtweaks.rtg;

import com.apocollis.chunksteward.api.ChunkStewardAPI;
import net.minecraftforge.fml.common.Loader;

/**
 * Whether the current work is a Chunk Steward pipeline job (a terrain worker, or a pipeline generator being
 * constructed on the server thread), and the per-job snapshots Chunk Steward hands the worker (API v3).
 * Worker generators are built on the server thread, so a thread check cannot tell them apart; the signal comes from
 * {@code ChunkStewardAPI.isPipelineContext()}. False when Chunk Steward is absent or older than API version 3.
 *
 * <p>Guards that read it: the generator stash in {@code MixinChunkGeneratorRTGVillage} and
 * {@code MixinMapGenVillageWorld} (a worker generator must never become the stashed one), {@code registerVillages}
 * (the layout runs in {@link VillagePlateJobPreparer#prepare} for a pipeline job), the registry write in
 * {@code MixinMapGenVillageStart}, and the flatten and seal passes (they read the job snapshot).
 *
 * <p>Every reference to a Chunk Steward class is in {@link Api}, which is only initialized after the runtime
 * {@code API_VERSION} was read as 3 or higher. Linking {@code isPipelineContext} from a class that also loads on a
 * version 2 jar would throw {@code NoSuchMethodError} at class load. The version is read by reflection because
 * {@code API_VERSION} is a compile-time constant that javac would inline.
 */
public final class PipelineContext {

    /** Greater than zero while {@link VillagePlateJobPreparer} runs on this thread: no API call from inside it. */
    private static final ThreadLocal<int[]> PREPARING = ThreadLocal.withInitial(() -> new int[1]);

    private static boolean registered;

    private PipelineContext() {}

    /** Lazy so the mod list is final when it is first read (first use is world load or later). */
    private static final class Availability {
        static final boolean API_V3 = detect();

        private static boolean detect() {
            try {
                if (!Loader.isModLoaded("chunksteward")) return false;
                Class<?> api = Class.forName("com.apocollis.chunksteward.api.ChunkStewardAPI");
                return api.getField("API_VERSION").getInt(null) >= 3;
            } catch (Throwable t) {
                return false;
            }
        }
    }

    /** The only class that links Chunk Steward API v3 methods. */
    private static final class Api {
        static boolean pipelineContext() {
            return ChunkStewardAPI.isPipelineContext();
        }

        static Object[] jobContext() {
            Object context = ChunkStewardAPI.currentJobContext();
            return context instanceof Object[] ? (Object[]) context : null;
        }

        static void register() {
            ChunkStewardAPI.registerJobPreparer(new VillagePlateJobPreparer());
        }
    }

    /** True when Chunk Steward with API version 3 or higher is loaded. */
    public static boolean apiV3() {
        return Availability.API_V3;
    }

    /**
     * True on a terrain worker, and on the server thread while Chunk Steward copies a worker generator. False on the
     * server thread during normal generation, on a verify thread, inside {@link VillagePlateJobPreparer}, and
     * whenever API v3 is not there.
     */
    public static boolean active() {
        if (!Availability.API_V3 || PREPARING.get()[0] > 0) return false;
        return Api.pipelineContext();
    }

    /**
     * The {@code prepare} results for the job on this worker, in registration order, or null. Look for the village
     * snapshot with {@code instanceof}, not by index. Only meaningful on a worker inside {@code generateChunk}.
     */
    public static Object[] jobContext() {
        if (!Availability.API_V3) return null;
        return Api.jobContext();
    }

    /** Registers the village-plate preparer with Chunk Steward once. Call from mod init. */
    public static synchronized void registerPreparer() {
        if (registered || !Availability.API_V3) return;
        registered = true;
        Api.register();
    }

    static void beginPrepare() {
        PREPARING.get()[0]++;
    }

    static void endPrepare() {
        int[] depth = PREPARING.get();
        if (depth[0] > 0) depth[0]--;
    }
}
