package com.apocollis.aqtweaks.world;

import com.apocollis.aqtweaks.ArcanaQuestTweaks;
import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.pacing.StewardPacing;
import com.apocollis.aqtweaks.pacing.TickSlice;
import com.apocollis.aqtweaks.perf.StageTimer;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import rtg.event.EventHandlerCommon;

import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Defers and dedupes RTG's {@code EventHandlerCommon.fixLighting(World, ChunkPos)}.
 *
 * <p>RTG fixes the four diagonal neighbours of every chunk it populates, each time, so a chunk's 256 columns can
 * be processed up to four times, and each column that trips its wool trick costs two light floods. Here the call is
 * recorded per chunk (a repeat call only refreshes the chunk's "last neighbour populated" tick) and run once after
 * the neighbourhood has been quiet, paced by the {@link StewardPacing} slice ({@link TickSlice}). With the adaptive
 * drain on, chunks in front of the player run first and unseen chunks wait for spare headroom. A chunk whose four
 * diagonal neighbours are already terrain-populated runs without the settle wait. Flushed for a chunk on unload
 * (before it is saved), for a world on world unload, for everything on server stop, and when an entry is older than
 * the age cap. RTG's own readiness check still decides whether the fix does anything.
 */
public final class RtgLightFixQueue {

    private static final int FLOOR_UNITS = 1;
    private static final int MAX_UNITS_NO_CLOCK = 2;
    private static final int MAX_UNITS_NO_CLOCK_PRIORITY = 6;
    /** Blocks per tick. Above this, travel direction replaces look direction. */
    private static final double MOVE_BLOCKS_PER_TICK = 0.15;
    /** One near-but-unseen chunk per second, and one unseen chunk per two seconds, when the slice is already full. */
    private static final int TIER1_LIVE_TICKS = 20;
    private static final int TIER2_LIVE_TICKS = 40;
    private static final double EWMA_KEEP = 0.8;
    /** Seeded from the profile average so the first chunks are not assumed free. */
    private static double predictedMs = 9.2;

    /** chunk (packed) -> world tick of the latest request; also remembers the first request tick in {@link #FIRST}. */
    private static final Map<World, Long2LongOpenHashMap> LAST = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<World, Long2LongOpenHashMap> FIRST = Collections.synchronizedMap(new WeakHashMap<>());
    private static final ThreadLocal<Boolean> BYPASS = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private RtgLightFixQueue() {}

    private static ArcanaQuestTweaksConfig.Surface cfg() {
        return ArcanaQuestTweaksConfig.RtgModuleConfig.surface;
    }

    /** Mixin hook. True when the call was queued and RTG's own run must be cancelled. */
    public static boolean intercept(World world, ChunkPos pos) {
        if (world == null || world.isRemote || BYPASS.get() || !cfg().deferRtgLightFix) return false;
        StageTimer.recordNs("rtg.lightFix.calls", 0L, null);
        if (playerWithin(world, pos.x, pos.z, cfg().rtgLightFixImmediateChunks)) {
            StageTimer.recordNs("rtg.lightFix.immediate", 0L, null);
            return false;
        }
        long key = ChunkPos.asLong(pos.x, pos.z);
        long now = world.getTotalWorldTime();
        Long2LongOpenHashMap last = map(LAST, world);
        Long2LongOpenHashMap first = map(FIRST, world);
        synchronized (last) {
            if (last.containsKey(key)) {
                StageTimer.recordNs("rtg.lightFix.repeat", 0L, null);
            } else {
                first.put(key, now);
            }
            last.put(key, now);
        }
        return true;
    }

    private static Long2LongOpenHashMap map(Map<World, Long2LongOpenHashMap> maps, World world) {
        synchronized (maps) {
            return maps.computeIfAbsent(world, w -> {
                Long2LongOpenHashMap m = new Long2LongOpenHashMap();
                m.defaultReturnValue(Long.MIN_VALUE);
                return m;
            });
        }
    }

    private static boolean playerWithin(World world, int chunkX, int chunkZ, int chunks) {
        if (chunks < 0) return false;
        for (EntityPlayer player : world.playerEntities) {
            if (Math.abs((MathHelper.floor(player.posX) >> 4) - chunkX) <= chunks
                    && Math.abs((MathHelper.floor(player.posZ) >> 4) - chunkZ) <= chunks) {
                return true;
            }
        }
        return false;
    }

    /** 0 = in the view cone or inside the immediate radius, 1 = near but outside the cone, 2 = unseen. */
    private static int tierOf(World world, int cx, int cz, ArcanaQuestTweaksConfig.Surface c) {
        EntityPlayer nearest = null;
        int nearestCheb = Integer.MAX_VALUE;
        for (EntityPlayer player : world.playerEntities) {
            int dx = Math.abs((MathHelper.floor(player.posX) >> 4) - cx);
            int dz = Math.abs((MathHelper.floor(player.posZ) >> 4) - cz);
            int cheb = Math.max(dx, dz);
            if (cheb < nearestCheb) {
                nearestCheb = cheb;
                nearest = player;
            }
        }
        if (nearest == null) return 2;
        if (c.rtgLightFixImmediateChunks >= 0 && nearestCheb <= c.rtgLightFixImmediateChunks) return 0;
        if (nearestCheb > c.rtgLightFixNearChunks) return 2;
        return inCone(nearest, cx, cz, c.rtgLightFixViewConeDegrees) ? 0 : 1;
    }

    private static boolean inCone(EntityPlayer player, int cx, int cz, int degrees) {
        if (degrees >= 360) return true;
        if (degrees <= 0) return false;
        double hx = (cx * 16 + 8) - player.posX;
        double hz = (cz * 16 + 8) - player.posZ;
        double hlen = Math.sqrt(hx * hx + hz * hz);
        if (hlen < 1.0e-4) return true;
        double mx = player.posX - player.prevPosX;
        double mz = player.posZ - player.prevPosZ;
        double speed = Math.sqrt(mx * mx + mz * mz);
        double dx;
        double dz;
        if (speed > MOVE_BLOCKS_PER_TICK) {
            dx = mx;
            dz = mz;
        } else {
            Vec3d look = player.getLookVec();
            dx = look.x;
            dz = look.z;
        }
        double dlen = Math.sqrt(dx * dx + dz * dz);
        if (dlen < 1.0e-4) return false;
        double dot = (hx / hlen) * (dx / dlen) + (hz / hlen) * (dz / dlen);
        return dot >= Math.cos(Math.toRadians(degrees * 0.5));
    }

    private static double distSq(World world, long key) {
        int cx = (int) key;
        int cz = (int) (key >> 32);
        double best = Double.MAX_VALUE;
        for (EntityPlayer player : world.playerEntities) {
            double dx = (MathHelper.floor(player.posX) >> 4) - cx;
            double dz = (MathHelper.floor(player.posZ) >> 4) - cz;
            best = Math.min(best, dx * dx + dz * dz);
        }
        return best == Double.MAX_VALUE ? Double.MAX_VALUE : best;
    }

    /** RTG's own readiness test: the four diagonal neighbours exist, and the chunk itself is loaded. */
    private static boolean ready(World world, int cx, int cz) {
        return world.getChunkProvider().getLoadedChunk(cx, cz) != null
                && world.isChunkGeneratedAt(cx + 1, cz + 1) && world.isChunkGeneratedAt(cx - 1, cz + 1)
                && world.isChunkGeneratedAt(cx + 1, cz - 1) && world.isChunkGeneratedAt(cx - 1, cz - 1);
    }

    /** All four diagonal neighbours are loaded and terrain-populated, so no further around-pass will enqueue this chunk. */
    private static boolean neighbourhoodReady(World world, int cx, int cz) {
        IChunkProvider provider = world.getChunkProvider();
        return provider.getLoadedChunk(cx, cz) != null
                && populated(provider, cx + 1, cz + 1)
                && populated(provider, cx - 1, cz + 1)
                && populated(provider, cx + 1, cz - 1)
                && populated(provider, cx - 1, cz - 1);
    }

    private static boolean populated(IChunkProvider provider, int cx, int cz) {
        Chunk chunk = provider.getLoadedChunk(cx, cz);
        return chunk != null && chunk.isTerrainPopulated();
    }

    private static void run(World world, long key) {
        BYPASS.set(Boolean.TRUE);
        try {
            EventHandlerCommon.fixLighting(world, new ChunkPos((int) key, (int) (key >> 32)));
        } finally {
            BYPASS.set(Boolean.FALSE);
        }
    }

    private static void forget(World world, long key) {
        Long2LongOpenHashMap last = LAST.get(world);
        Long2LongOpenHashMap first = FIRST.get(world);
        if (last != null) synchronized (last) { last.remove(key); }
        if (first != null) synchronized (first) { first.remove(key); }
    }

    // --- per-tick drain ---

    private static int tickId = Integer.MIN_VALUE;
    private static boolean clock;
    private static boolean fullTps;
    private static double normalMs;
    private static double nearMs;
    private static int lastLiveTick1 = Integer.MIN_VALUE;
    private static int lastLiveTick2 = Integer.MIN_VALUE;

    private static void beginSlice(MinecraftServer server) {
        int t = server == null ? tickId : server.getTickCounter();
        if (server != null && t == tickId) return;
        tickId = t;
        ArcanaQuestTweaksConfig.Surface c = cfg();
        double n = StewardPacing.sliceBudgetMs(c.rtgLightFixMaxSliceMs, 0.5);
        double f = StewardPacing.sliceBudgetMs(Math.max(c.rtgLightFixMaxSliceMs, c.rtgLightFixNearSliceMs), 0.5);
        clock = n >= 0.0;
        normalMs = n;
        nearMs = Math.max(n, f);
        fullTps = !clock || StewardPacing.getLoadFactor() >= 1.0;
        TickSlice.start(server);
    }

    private static boolean allowFixed(MinecraftServer server, int units, boolean priority) {
        if (units < FLOOR_UNITS) return true;
        if (!clock) return units < (priority ? MAX_UNITS_NO_CLOCK_PRIORITY : MAX_UNITS_NO_CLOCK);
        double limit = priority ? nearMs : normalMs;
        if (limit <= 0.0 || TickSlice.elapsedMs(server) >= limit) return false;
        return fullTps || StewardPacing.remainingMs(40.0) >= 2.0;
    }

    /**
     * Adaptive start. Tier 0 always gets one chunk in the tick. Further chunks start only when the predicted
     * cost fits the remaining slice. Below full TPS only that tier-0 floor runs.
     */
    private static boolean allowAdaptive(MinecraftServer server, int tier, int ranThisTier, ArcanaQuestTweaksConfig.Surface c) {
        if (tier == 0 && ranThisTier < FLOOR_UNITS) return true;
        if (!fullTps) return false;
        double rem = StewardPacing.remainingMs(40.0);
        if (tier == 2 && rem < c.rtgLightFixUnseenMinHeadroomMs) return liveFloor(tier);
        double capMs = tier == 0 ? c.rtgLightFixNearSliceMs : c.rtgLightFixMaxSliceMs;
        double limit = Math.min(capMs, Math.max(0.0, rem - c.rtgLightFixReserveMs));
        if (TickSlice.elapsedMs(server) + predictedMs <= limit) return true;
        return tier > 0 && liveFloor(tier);
    }

    private static boolean liveFloor(int tier) {
        int last = tier == 1 ? lastLiveTick1 : lastLiveTick2;
        int interval = tier == 1 ? TIER1_LIVE_TICKS : TIER2_LIVE_TICKS;
        return tickId - last >= interval;
    }

    private static void noteTierRun(int tier) {
        if (tier == 1) lastLiveTick1 = tickId;
        else if (tier == 2) lastLiveTick2 = tickId;
    }

    private static void tick(World world) {
        Long2LongOpenHashMap last = LAST.get(world);
        if (last == null) return;
        long now = world.getTotalWorldTime();
        ArcanaQuestTweaksConfig.Surface c = cfg();
        long[] keys;
        synchronized (last) {
            if (last.isEmpty()) return;
            keys = last.keySet().toLongArray();
        }
        MinecraftServer server = world.getMinecraftServer();
        beginSlice(server);
        boolean adaptive = c.rtgLightFixAdaptiveSlice;
        long t0 = System.nanoTime();
        int n = keys.length;
        int[] tiers = new int[n];
        double[] dist = new double[n];
        Integer[] order = new Integer[n];
        int[] pendingTier = new int[3];
        for (int i = 0; i < n; i++) {
            int cx = (int) keys[i];
            int cz = (int) (keys[i] >> 32);
            tiers[i] = tierOf(world, cx, cz, c);
            if (tiers[i] >= 0 && tiers[i] < 3) pendingTier[tiers[i]]++;
            dist[i] = distSq(world, keys[i]);
            order[i] = i;
        }
        Arrays.sort(order, (a, b) -> {
            if (adaptive) {
                int byTier = Integer.compare(tiers[a], tiers[b]);
                if (byTier != 0) return byTier;
            }
            return Double.compare(dist[a], dist[b]);
        });
        StageTimer.recordNs("rtg.lightFix.pending.t0", pendingTier[0] * 1_000_000L, null);
        StageTimer.recordNs("rtg.lightFix.pending.t1", pendingTier[1] * 1_000_000L, null);
        StageTimer.recordNs("rtg.lightFix.pending.t2", pendingTier[2] * 1_000_000L, null);

        Long2LongOpenHashMap first = FIRST.get(world);
        int units = 0;
        int[] ran = new int[3];
        int near = c.rtgLightFixNearChunks;
        for (int idx = 0; idx < n; idx++) {
            int slot = order[idx];
            long key = keys[slot];
            long lastTick;
            long firstTick;
            synchronized (last) {
                lastTick = last.get(key);
                firstTick = first == null ? now : first.get(key);
            }
            if (lastTick == Long.MIN_VALUE) continue;
            int cx = (int) key;
            int cz = (int) (key >> 32);
            int tier = tiers[slot];
            boolean priority = tier < 2 && playerWithin(world, cx, cz, near);
            boolean complete = c.rtgLightFixNeighbourhoodTrigger && neighbourhoodReady(world, cx, cz);
            long settle;
            if (complete) {
                settle = 0L;
            } else if (!adaptive) {
                settle = priority ? Math.min(c.rtgLightFixSettleTicks, 10) : c.rtgLightFixSettleTicks;
            } else if (tier == 2) {
                settle = c.rtgLightFixUnseenSettleTicks;
            } else {
                settle = Math.min(c.rtgLightFixSettleTicks, 10);
            }
            boolean expired = now - firstTick > c.rtgLightFixMaxAgeTicks;
            if (!expired && now - lastTick < settle) continue;
            if (!expired && !complete && !ready(world, cx, cz)) continue;
            if (!expired) {
                boolean start = adaptive && clock
                        ? allowAdaptive(server, tier, ran[tier], c)
                        : allowFixed(server, units, adaptive ? tier == 0 : priority);
                if (!start) continue;
            }
            forget(world, key);
            long u0 = System.nanoTime();
            run(world, key);
            long took = System.nanoTime() - u0;
            units++;
            if (tier >= 0 && tier < 3) ran[tier]++;
            noteTierRun(tier);
            double tookMs = took / 1_000_000.0;
            predictedMs = predictedMs * EWMA_KEEP + tookMs * (1.0 - EWMA_KEEP);
            long waited = firstTick == Long.MIN_VALUE ? 0L : Math.max(0L, now - firstTick);
            StageTimer.recordNs("rtg.lightFix.run", took, "t" + tier);
            StageTimer.recordNs("rtg.lightFix.latency.t" + tier, waited * 1_000_000L, null);
        }
        double usedMs = (System.nanoTime() - t0) / 1_000_000.0;
        double rem = StewardPacing.remainingMs(40.0);
        double granted = adaptive
                ? Math.min(c.rtgLightFixNearSliceMs, Math.max(0.0, rem - c.rtgLightFixReserveMs))
                : (clock ? nearMs : 0.0);
        StageTimer.recordNs("rtg.lightFix.budget", (long) (usedMs * 1_000_000.0),
                String.format(Locale.ROOT, "granted=%.1f", granted));
        if (units > 0) {
            StageTimer.recordNs("rtg.lightFix.drain", System.nanoTime() - t0,
                    "units=" + units + ",pending=" + pending(last)
                            + ",t0=" + ran[0] + ",t1=" + ran[1] + ",t2=" + ran[2]);
        }
    }

    private static int pending(Long2LongOpenHashMap last) {
        synchronized (last) {
            return last.size();
        }
    }

    private static void flushChunk(World world, int cx, int cz) {
        Long2LongOpenHashMap last = LAST.get(world);
        if (last == null) return;
        long key = ChunkPos.asLong(cx, cz);
        boolean has;
        synchronized (last) {
            has = last.containsKey(key);
        }
        if (!has) return;
        forget(world, key);
        run(world, key);
        StageTimer.recordNs("rtg.lightFix.unloadFlush", 0L, null);
    }

    private static void flushWorld(World world) {
        Long2LongOpenHashMap last = LAST.get(world);
        if (last == null) return;
        long[] keys;
        synchronized (last) {
            keys = last.keySet().toLongArray();
        }
        for (long key : keys) {
            forget(world, key);
            if (ready(world, (int) key, (int) (key >> 32))) {
                run(world, key);
            }
        }
    }

    /** Server stop: everything pending, every world, before chunks are saved. */
    public static void flushAll() {
        World[] worlds;
        synchronized (LAST) {
            worlds = LAST.keySet().toArray(new World[0]);
        }
        for (World world : worlds) {
            flushWorld(world);
        }
    }

    @Mod.EventBusSubscriber(modid = ArcanaQuestTweaks.MODID)
    public static final class Events {
        @SubscribeEvent
        public static void onWorldTick(TickEvent.WorldTickEvent event) {
            if (event.phase != TickEvent.Phase.END || event.world == null || event.world.isRemote) return;
            tick(event.world);
        }

        @SubscribeEvent
        public static void onChunkUnload(ChunkEvent.Unload event) {
            World world = event.getWorld();
            Chunk chunk = event.getChunk();
            if (world == null || world.isRemote || chunk == null) return;
            flushChunk(world, chunk.x, chunk.z);
        }

        @SubscribeEvent
        public static void onWorldUnload(WorldEvent.Unload event) {
            World world = event.getWorld();
            if (world == null || world.isRemote) return;
            flushWorld(world);
        }
    }
}
