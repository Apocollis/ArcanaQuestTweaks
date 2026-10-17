package com.apocollis.aqtweaks.rtg;

import com.apocollis.aqtweaks.ArcanaQuestTweaks;
import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.mixin.vanilla.AccessorChunkProviderServer;
import com.apocollis.aqtweaks.perf.StageTimer;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Per-dimension server-side caches for village well evaluation:
 * <ul>
 *   <li><b>B1 ({@link #chunkExists}):</b> peeks {@code ChunkProviderServer.loadedChunks} without
 *       touching {@code unloadQueued}, then checks a permanent positive set and a short-TTL
 *       negative map before falling back to {@code World.isChunkGeneratedAt}.</li>
 *   <li><b>B2 ({@link #startRejectReason} and {@link #findDryWell}):</b> short-TTL memoization
 *       keyed by {@code (dimension, seed, configFingerprint)} and coordinates, with
 *       {@link StageTimer} distinct vs repeat counters and an optional A/B verification mode.</li>
 * </ul>
 */
public final class VillageWellCache {

    private static final Logger LOGGER = LogManager.getLogger(ArcanaQuestTweaks.MODID);
    private static final int MAX_CHUNK_CACHE_ENTRIES = 65536;
    private static final int MAX_MEMO_ENTRIES = 16384;

    private static final ConcurrentHashMap<Integer, DimState> BY_DIM = new ConcurrentHashMap<>();

    private static final class RejectEntry {
        final String reason;
        final long expireTick;

        RejectEntry(String reason, long expireTick) {
            this.reason = reason;
            this.expireTick = expireTick;
        }
    }

    private static final class DryWellEntry {
        final int[] dryXZ;
        final long expireTick;

        DryWellEntry(int[] dryXZ, long expireTick) {
            this.dryXZ = dryXZ != null ? new int[] {dryXZ[0], dryXZ[1]} : null;
            this.expireTick = expireTick;
        }

        int[] copyResult() {
            return dryXZ != null ? new int[] {dryXZ[0], dryXZ[1]} : null;
        }
    }

    private static final class DimState {
        final int dim;
        final long seed;
        int configFingerprint;

        final LongOpenHashSet positiveChunks = new LongOpenHashSet();
        final Long2LongOpenHashMap negativeChunks = new Long2LongOpenHashMap();

        final Long2ObjectOpenHashMap<RejectEntry> rejectMemo = new Long2ObjectOpenHashMap<>();
        final Long2ObjectOpenHashMap<DryWellEntry> dryWellMemo = new Long2ObjectOpenHashMap<>();

        final LongOpenHashSet seenRejectCoords = new LongOpenHashSet();
        final LongOpenHashSet seenDryWellCoords = new LongOpenHashSet();
        final LongOpenHashSet verifyLoggedCoords = new LongOpenHashSet();
        long rejectCalls;
        long rejectRepeats;
        long rejectMemoHits;
        long dryWellCalls;
        long dryWellRepeats;
        long dryWellMemoHits;

        DimState(int dim, long seed, int configFingerprint) {
            this.dim = dim;
            this.seed = seed;
            this.configFingerprint = configFingerprint;
            this.negativeChunks.defaultReturnValue(Long.MIN_VALUE);
        }

        void checkConfig(int currentFingerprint) {
            if (this.configFingerprint != currentFingerprint) {
                this.configFingerprint = currentFingerprint;
                this.rejectMemo.clear();
                this.dryWellMemo.clear();
            }
        }
    }

    private VillageWellCache() {}

    public static void invalidateAll() {
        BY_DIM.clear();
    }

    public static void invalidateWorld(World world) {
        if (world == null || world.isRemote || world.provider == null) return;
        int dim = world.provider.getDimension();
        BY_DIM.remove(dim);
        if (dim == 0) {
            long seed = safeSeed(world);
            BY_DIM.entrySet().removeIf(e -> e.getValue().seed == seed);
        }
    }

    public static boolean chunkExists(World world, int cx, int cz) {
        if (world == null) return false;
        if (world.isRemote
                || !ArcanaQuestTweaksConfig.RtgModuleConfig.surface.villageWellCacheEnabled
                || !isServerThread(world)) {
            return rawChunkExists(world, cx, cz);
        }

        long key = ChunkPos.asLong(cx, cz);
        IChunkProvider provider = world.getChunkProvider();
        if (provider instanceof AccessorChunkProviderServer accessor) {
            Long2ObjectMap<Chunk> loaded = accessor.aqtweaks$getLoadedChunks();
            if (loaded != null && loaded.containsKey(key)) {
                DimState state = stateFor(world);
                synchronized (state) {
                    putPositive(state, key);
                }
                return true;
            }
        }

        DimState state = stateFor(world);
        long now = world.getTotalWorldTime();
        int negTtl = Math.max(0, ArcanaQuestTweaksConfig.RtgModuleConfig.surface.villageWellNegativeTtlTicks);

        synchronized (state) {
            if (state.positiveChunks.contains(key)) {
                return true;
            }
            if (negTtl > 0) {
                long exp = state.negativeChunks.get(key);
                if (exp != Long.MIN_VALUE) {
                    if (now <= exp && exp - now <= negTtl) {
                        return false;
                    }
                    state.negativeChunks.remove(key);
                }
            }
        }

        boolean exists = rawChunkExists(world, cx, cz);
        synchronized (state) {
            if (exists) {
                putPositive(state, key);
            } else if (negTtl > 0) {
                if (state.negativeChunks.size() >= MAX_CHUNK_CACHE_ENTRIES) {
                    state.negativeChunks.clear();
                }
                state.negativeChunks.put(key, now + negTtl);
            }
        }
        return exists;
    }

    public static String startRejectReason(World world, int chunkX, int chunkZ, Supplier<String> computer) {
        if (world == null) return null;
        long t0 = StageTimer.start();
        boolean server = !world.isRemote && isServerThread(world);
        if (!server) {
            String res = computer.get();
            if (t0 != 0L) {
                StageTimer.end("village.wellReject", t0, chunkX + "," + chunkZ);
            }
            return res;
        }

        DimState state = stateFor(world);
        long key = ChunkPos.asLong(chunkX, chunkZ);
        long now = world.getTotalWorldTime();
        int fp = configFingerprint();
        int ttl = Math.max(0, ArcanaQuestTweaksConfig.RtgModuleConfig.surface.villageWellMemoTtlTicks);
        boolean memoOn = ArcanaQuestTweaksConfig.RtgModuleConfig.surface.villageWellMemoEnabled && ttl > 0;
        boolean verify = ArcanaQuestTweaksConfig.RtgModuleConfig.surface.villageWellMemoVerify;
        boolean hasStart = VillageLandHelper.villageStartAt(world, chunkX, chunkZ);

        RejectEntry hit = null;
        long distinct;
        long repeats;
        long hits;
        synchronized (state) {
            state.checkConfig(fp);
            state.rejectCalls++;
            if (state.seenRejectCoords.size() >= MAX_MEMO_ENTRIES) {
                state.seenRejectCoords.clear();
            }
            if (!state.seenRejectCoords.add(key)) {
                state.rejectRepeats++;
            }
            if (memoOn) {
                RejectEntry cached = state.rejectMemo.get(key);
                if (cached != null) {
                    boolean validTick = now <= cached.expireTick && cached.expireTick - now <= ttl;
                    boolean validAllow = cached.reason != null || !hasStart;
                    if (validTick && validAllow) {
                        hit = cached;
                        state.rejectMemoHits++;
                    } else {
                        state.rejectMemo.remove(key);
                    }
                }
            }
            distinct = state.seenRejectCoords.size();
            repeats = state.rejectRepeats;
            hits = state.rejectMemoHits;
        }

        if (hit != null) {
            if (verify) {
                String fresh = computer.get();
                if (!Objects.equals(hit.reason, fresh)) {
                    logVerifyMismatch(state, key,
                            "[AQ-RTG] startRejectReason memo mismatch at chunk=" + chunkX + "," + chunkZ
                                    + " cached=" + hit.reason + " fresh=" + fresh);
                }
            }
            if (t0 != 0L) {
                StageTimer.end("village.wellReject", t0,
                        chunkX + "," + chunkZ + " distinct=" + distinct + " repeats=" + repeats + " hits=" + hits);
            }
            return hit.reason;
        }

        String computed = computer.get();
        if (memoOn && (computed != null || !hasStart)) {
            synchronized (state) {
                if (state.rejectMemo.size() >= MAX_MEMO_ENTRIES) {
                    state.rejectMemo.clear();
                }
                state.rejectMemo.put(key, new RejectEntry(computed, now + ttl));
            }
        }
        if (t0 != 0L) {
            StageTimer.end("village.wellReject", t0,
                    chunkX + "," + chunkZ + " distinct=" + distinct + " repeats=" + repeats + " hits=" + hits);
        }
        return computed;
    }

    public static int[] findDryWell(World world, int wellX, int wellZ, Supplier<int[]> computer) {
        if (world == null) return null;
        long t0 = StageTimer.start();
        boolean server = !world.isRemote && isServerThread(world);
        if (!server) {
            int[] res = computer.get();
            if (t0 != 0L) {
                StageTimer.end("village.findDryWell", t0, wellX + "," + wellZ);
            }
            return res;
        }

        DimState state = stateFor(world);
        long key = packXZ(wellX, wellZ);
        long now = world.getTotalWorldTime();
        int fp = configFingerprint();
        int ttl = Math.max(0, ArcanaQuestTweaksConfig.RtgModuleConfig.surface.villageWellMemoTtlTicks);
        boolean memoOn = ArcanaQuestTweaksConfig.RtgModuleConfig.surface.villageWellMemoEnabled && ttl > 0;
        boolean verify = ArcanaQuestTweaksConfig.RtgModuleConfig.surface.villageWellMemoVerify;

        DryWellEntry hit = null;
        long distinct;
        long repeats;
        long hits;
        synchronized (state) {
            state.checkConfig(fp);
            state.dryWellCalls++;
            if (state.seenDryWellCoords.size() >= MAX_MEMO_ENTRIES) {
                state.seenDryWellCoords.clear();
            }
            if (!state.seenDryWellCoords.add(key)) {
                state.dryWellRepeats++;
            }
            if (memoOn) {
                DryWellEntry cached = state.dryWellMemo.get(key);
                if (cached != null) {
                    if (now <= cached.expireTick && cached.expireTick - now <= ttl) {
                        hit = cached;
                        state.dryWellMemoHits++;
                    } else {
                        state.dryWellMemo.remove(key);
                    }
                }
            }
            distinct = state.seenDryWellCoords.size();
            repeats = state.dryWellRepeats;
            hits = state.dryWellMemoHits;
        }

        if (hit != null) {
            int[] cachedOut = hit.copyResult();
            if (verify) {
                int[] fresh = computer.get();
                if (!Arrays.equals(cachedOut, fresh)) {
                    logVerifyMismatch(state, key,
                            "[AQ-RTG] findDryWell memo mismatch at well=" + wellX + "," + wellZ
                                    + " cached=" + Arrays.toString(cachedOut) + " fresh=" + Arrays.toString(fresh));
                }
            }
            if (t0 != 0L) {
                StageTimer.end("village.findDryWell", t0,
                        wellX + "," + wellZ + " distinct=" + distinct + " repeats=" + repeats + " hits=" + hits);
            }
            return cachedOut;
        }

        int[] computed = computer.get();
        if (memoOn) {
            synchronized (state) {
                if (state.dryWellMemo.size() >= MAX_MEMO_ENTRIES) {
                    state.dryWellMemo.clear();
                }
                state.dryWellMemo.put(key, new DryWellEntry(computed, now + ttl));
            }
        }
        if (t0 != 0L) {
            StageTimer.end("village.findDryWell", t0,
                    wellX + "," + wellZ + " distinct=" + distinct + " repeats=" + repeats + " hits=" + hits);
        }
        return computed;
    }

    private static void putPositive(DimState state, long key) {
        if (state.positiveChunks.size() >= MAX_CHUNK_CACHE_ENTRIES) {
            state.positiveChunks.clear();
        }
        state.positiveChunks.add(key);
        state.negativeChunks.remove(key);
    }

    private static boolean rawChunkExists(World world, int cx, int cz) {
        try {
            return world.isChunkGeneratedAt(cx, cz);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean isServerThread(World world) {
        if (world instanceof WorldServer ws) {
            return ws.isCallingFromMinecraftThread();
        }
        if (world.getMinecraftServer() != null) {
            return world.getMinecraftServer().isCallingFromMinecraftThread();
        }
        return false;
    }

    private static DimState stateFor(World world) {
        int dim = world.provider != null ? world.provider.getDimension() : 0;
        long seed = safeSeed(world);
        int fp = configFingerprint();
        return BY_DIM.compute(dim, (d, existing) -> {
            if (existing == null || existing.seed != seed) {
                return new DimState(d, seed, fp);
            }
            return existing;
        });
    }

    private static long safeSeed(World world) {
        try {
            return world.getSeed();
        } catch (Throwable ignored) {
            return 0L;
        }
    }

    private static int configFingerprint() {
        ArcanaQuestTweaksConfig.Surface s = ArcanaQuestTweaksConfig.RtgModuleConfig.surface;
        int h = s.villageWaterRetryDistance;
        h = 31 * h + (s.rejectCoastalVillageStarts ? 1 : 0);
        h = 31 * h + s.villageCoastBuffer;
        h = 31 * h + s.villageMinWellHeight;
        return h;
    }

    private static long packXZ(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    private static void logVerifyMismatch(DimState state, long key, String message) {
        boolean shouldLog;
        synchronized (state) {
            if (state.verifyLoggedCoords.size() >= MAX_MEMO_ENTRIES) {
                state.verifyLoggedCoords.clear();
            }
            shouldLog = state.verifyLoggedCoords.add(key);
        }
        if (shouldLog) {
            LOGGER.warn(message);
        }
    }
}
