package com.apocollis.aqtweaks.world;

import com.apocollis.aqtweaks.ArcanaQuestTweaks;
import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.pacing.StewardPacing;
import com.apocollis.aqtweaks.pacing.TickSlice;
import com.apocollis.aqtweaks.perf.StageTimer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * The server-tick drain for {@link PopulateRelight}. A populated chunk whose light was recorded waits here, held
 * unsent (its {@code isLightPopulated} is false) until its replay runs. Chunks within {@code Populate Relight
 * Immediate Chunks} of a player are replayed at populate return; chunks within {@code Populate Relight Near Chunks}
 * drain first with the larger near-player slice; each tick the drain runs under the {@link StewardPacing} slice, nearest
 * chunk first, with a floor of one chunk per tick so it always progresses. Pending work runs before the chunk is saved
 * (unload), when its world unloads or the server stops, and when it is older than {@code Populate Relight Max Age Ticks}.
 */
public final class PopulateRelightQueue {

    /** A chunk replay is one unit and costs tens of milliseconds, so the guaranteed floor is one per tick. */
    private static final int FLOOR_UNITS = 1;
    private static final int MAX_UNITS_NO_CLOCK = 4;
    private static final int MAX_UNITS_NO_CLOCK_PRIORITY = 16;

    private static final Map<World, Map<Long, PopulateRelight.Record>> QUEUES =
            Collections.synchronizedMap(new WeakHashMap<>());

    private static final class SliceClock {
        int tick = Integer.MIN_VALUE;
        boolean clock;
        boolean fullTps;
        double ms;
        double nearMs;
        long startNs;
    }

    private static final SliceClock SLICE = new SliceClock();

    private PopulateRelightQueue() {}

    private static ArcanaQuestTweaksConfig.DepthsModuleConfig.General cfg() {
        return ArcanaQuestTweaksConfig.DepthsModuleConfig.general;
    }

    /** Takes one chunk's record at populate return: replay now when a player is that close, else queue it. */
    static void take(World world, PopulateRelight.Record record) {
        Chunk chunk = record.chunk;
        int immediate = cfg().populateRelightImmediateChunks;
        if (immediate >= 0 && playerWithin(world, chunk.x, chunk.z, immediate)) {
            PopulateRelight.replay(record);
            StageTimer.recordNs("populate.immediate", 0L, null);
            return;
        }
        record.queued = true;
        record.queuedTick = world.getTotalWorldTime();
        Map<Long, PopulateRelight.Record> queue;
        synchronized (QUEUES) {
            queue = QUEUES.computeIfAbsent(world, w -> new LinkedHashMap<>());
        }
        synchronized (queue) {
            queue.put(ChunkPos.asLong(chunk.x, chunk.z), record);
        }
        StageTimer.recordNs("populate.queued", 0L, "cols=" + record.columns.size() + ",checks=" + record.checks.size());
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

    private static double nearestPlayerDistSq(World world, int cx, int cz) {
        double best = Double.MAX_VALUE;
        for (EntityPlayer player : world.playerEntities) {
            double dx = (MathHelper.floor(player.posX) >> 4) - cx;
            double dz = (MathHelper.floor(player.posZ) >> 4) - cz;
            best = Math.min(best, dx * dx + dz * dz);
        }
        return best == Double.MAX_VALUE ? 1.0e12 : best;
    }

    private static void beginSlice(World world) {
        MinecraftServer server = world.getMinecraftServer();
        int tick = server == null ? SLICE.tick : server.getTickCounter();
        if (server != null && SLICE.tick == tick) return;
        double budget = StewardPacing.sliceBudgetMs(cfg().deferTemplateRelightMaxSliceMs, 0.5);
        double nearBudget = StewardPacing.sliceBudgetMs(
                Math.max(cfg().deferTemplateRelightMaxSliceMs, cfg().deferTemplateRelightNearSliceMs), 0.5);
        SLICE.tick = tick;
        if (budget < 0.0) {
            SLICE.clock = false;
            SLICE.fullTps = false;
            SLICE.ms = 0.0;
            SLICE.nearMs = 0.0;
            SLICE.startNs = 0L;
            return;
        }
        SLICE.clock = true;
        SLICE.fullTps = StewardPacing.getLoadFactor() >= 1.0;
        SLICE.ms = budget;
        SLICE.nearMs = Math.max(budget, nearBudget);
        SLICE.startNs = TickSlice.start(server);
    }

    private static boolean allow(int units, boolean priority) {
        if (units < FLOOR_UNITS) return true;
        if (!SLICE.clock) return units < (priority ? MAX_UNITS_NO_CLOCK_PRIORITY : MAX_UNITS_NO_CLOCK);
        double limit = priority ? SLICE.nearMs : SLICE.ms;
        if (limit <= 0.0) return false;
        if ((System.nanoTime() - SLICE.startNs) / 1_000_000.0 >= limit) return false;
        return SLICE.fullTps || StewardPacing.remainingMs(40.0) >= 2.0;
    }

    private static void tick(World world) {
        Map<Long, PopulateRelight.Record> queue = QUEUES.get(world);
        if (queue == null) return;
        List<PopulateRelight.Record> records;
        synchronized (queue) {
            if (queue.isEmpty()) return;
            records = new ArrayList<>(queue.values());
        }
        beginSlice(world);
        long t0 = System.nanoTime();
        long now = world.getTotalWorldTime();
        int near = cfg().populateRelightNearChunks;
        int maxAge = cfg().populateRelightMaxAgeTicks;
        int n = records.size();
        final double[] dist = new double[n];
        Integer[] order = new Integer[n];
        for (int i = 0; i < n; i++) {
            Chunk c = records.get(i).chunk;
            dist[i] = nearestPlayerDistSq(world, c.x, c.z);
            order[i] = i;
        }
        Arrays.sort(order, (a, b) -> Double.compare(dist[a], dist[b]));
        int units = 0;
        int expired = 0;
        for (int slot = 0; slot < n; slot++) {
            PopulateRelight.Record record = records.get(order[slot]);
            Chunk chunk = record.chunk;
            boolean old = now - record.queuedTick > maxAge;
            boolean priority = playerWithin(world, chunk.x, chunk.z, near);
            if (!old && !allow(units, priority)) {
                if (!priority) break;
                continue;
            }
            synchronized (queue) {
                queue.remove(ChunkPos.asLong(chunk.x, chunk.z));
            }
            long u0 = System.nanoTime();
            PopulateRelight.replay(record);
            StageTimer.recordNs("populate.replay", System.nanoTime() - u0, priority ? "near" : "far");
            StageTimer.recordNs("populate.latency", 0L, "ticks=" + (now - record.queuedTick));
            units++;
            if (old) expired++;
        }
        if (units > 0) {
            int pending;
            synchronized (queue) {
                pending = queue.size();
            }
            StageTimer.recordNs("populate.drain", System.nanoTime() - t0,
                    "chunks=" + units + ",expired=" + expired + ",pending=" + pending);
        }
    }

    private static void flushChunk(World world, Chunk chunk) {
        PopulateRelight.Record record = ((PopulateRelight.Pending) chunk).aqtweaks$pending();
        if (record == null) return;
        Map<Long, PopulateRelight.Record> queue = QUEUES.get(world);
        if (queue != null) {
            synchronized (queue) {
                queue.remove(ChunkPos.asLong(chunk.x, chunk.z));
            }
        }
        PopulateRelight.replay(record);
        StageTimer.recordNs("populate.unloadFlush", 0L, "chunk=" + chunk.x + "," + chunk.z);
    }

    private static void flushWorld(World world) {
        Map<Long, PopulateRelight.Record> queue = QUEUES.remove(world);
        if (queue == null) return;
        List<PopulateRelight.Record> records;
        synchronized (queue) {
            records = new ArrayList<>(queue.values());
            queue.clear();
        }
        for (PopulateRelight.Record record : records) {
            PopulateRelight.replay(record);
        }
    }

    /** Server stop: everything pending, every world, before chunks are saved. */
    public static void flushAll() {
        World[] worlds;
        synchronized (QUEUES) {
            worlds = QUEUES.keySet().toArray(new World[0]);
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
            if (world == null || world.isRemote) return;
            flushChunk(world, event.getChunk());
        }

        @SubscribeEvent
        public static void onWorldUnload(WorldEvent.Unload event) {
            World world = event.getWorld();
            if (world == null || world.isRemote) return;
            flushWorld(world);
        }
    }
}
