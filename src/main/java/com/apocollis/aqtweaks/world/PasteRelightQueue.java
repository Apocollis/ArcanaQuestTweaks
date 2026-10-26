package com.apocollis.aqtweaks.world;

import com.apocollis.aqtweaks.ArcanaQuestTweaks;
import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.pacing.StewardPacing;
import com.apocollis.aqtweaks.pacing.TickSlice;
import com.apocollis.aqtweaks.perf.StageTimer;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Relight work recorded by {@link BulkTemplateRelight}, drained over later server ticks instead of inside
 * the paste tick. A Dungeon Additions High City paste spent 13.2 s of its 16 s replaying relights.
 *
 * <p>One {@link Batch} per paste: phase 1 relights every recorded column (height maps become final), phase 2
 * runs the recorded light checks, the same order the immediate replay used. Each tick the drain runs under the
 * {@link StewardPacing} slice, nearest chunks to a player first, with a small floor so it always progresses.
 *
 * <p>Pending work leaves the height map and skylight of those columns stale, so it is flushed (a) at once when
 * any chunk of the paste is near a player, (b) for a chunk on {@link ChunkEvent.Unload}, which fires before the
 * chunk is saved, (c) for a world on world unload and for everything on server stop, and (d) when a batch is
 * older than the configured age. {@code relightBlock} recomputes the column from its blocks, so replaying later
 * gives the same result as replaying at paste end.
 */
public final class PasteRelightQueue {

    private static final int FLOOR_UNITS = 2;
    private static final int MAX_UNITS_NO_CLOCK = 24;
    private static final int MAX_UNITS_NO_CLOCK_PRIORITY = 96;

    private static final class Batch {
        final long queuedTick;
        final Long2ObjectOpenHashMap<Int2IntOpenHashMap> columns = new Long2ObjectOpenHashMap<>();
        LongArrayList checks = new LongArrayList();
        int nextCheck;
        final int totalColumns;
        final int totalChecks;
        /** A player is near: drains first and with the larger near-player slice. */
        boolean priority;

        Batch(long queuedTick, int totalColumns, int totalChecks) {
            this.queuedTick = queuedTick;
            this.totalColumns = totalColumns;
            this.totalChecks = totalChecks;
        }
    }

    private static final Map<World, ArrayDeque<Batch>> QUEUES = Collections.synchronizedMap(new WeakHashMap<>());

    /** One slice per server tick, shared by every dimension. */
    private static final class SliceClock {
        int tick = Integer.MIN_VALUE;
        boolean clock;
        boolean fullTps;
        double ms;
        double nearMs;
        long startNs;
    }

    private static final SliceClock SLICE = new SliceClock();

    private PasteRelightQueue() {}

    private static ArcanaQuestTweaksConfig.DepthsModuleConfig.General cfg() {
        return ArcanaQuestTweaksConfig.DepthsModuleConfig.general;
    }

    /**
     * Takes the work of one paste. Columns in chunks within {@code Immediate Chunks} of a player are relit now (the
     * player can see them). A paste touching a chunk within {@code Near Chunks} of a player is queued as a priority
     * batch: it drains first, with the larger near-player slice, instead of freezing the paste tick. Everything else
     * is queued at normal priority. {@code columns} maps a chunk to (column index, highest Y); {@code checks} are
     * packed positions in order.
     */
    static void enqueue(World world, Map<Chunk, Int2IntOpenHashMap> columns, LongArrayList checks) {
        int totalColumns = 0;
        for (Int2IntOpenHashMap m : columns.values()) {
            totalColumns += m.size();
        }
        if (totalColumns == 0 && checks.isEmpty()) return;
        Batch batch = new Batch(world.getTotalWorldTime(), totalColumns, checks.size());
        int near = cfg().deferTemplateRelightNearChunks;
        int immediate = cfg().deferTemplateRelightImmediateChunks;
        int immediateCols = 0;
        for (Map.Entry<Chunk, Int2IntOpenHashMap> e : columns.entrySet()) {
            Chunk chunk = e.getKey();
            if (immediate >= 0 && playerWithin(world, chunk.x, chunk.z, immediate)) {
                for (Int2IntMap.Entry col : e.getValue().int2IntEntrySet()) {
                    relightColumn(chunk, col.getIntKey(), col.getIntValue());
                    immediateCols++;
                }
                continue;
            }
            batch.columns.put(ChunkPos.asLong(chunk.x, chunk.z), e.getValue());
            batch.priority |= playerWithin(world, chunk.x, chunk.z, near);
        }
        for (int i = 0; i < checks.size() && !batch.priority; i++) {
            BlockPos pos = BlockPos.fromLong(checks.getLong(i));
            batch.priority = playerWithin(world, pos.getX() >> 4, pos.getZ() >> 4, near);
        }
        batch.checks = checks;
        StageTimer.recordNs("paste.queued", 0L, "cols=" + totalColumns + ",checks=" + checks.size()
                + ",immediate=" + immediateCols + (batch.priority ? ",priority" : ""));
        ArrayDeque<Batch> queue;
        synchronized (QUEUES) {
            queue = QUEUES.computeIfAbsent(world, w -> new ArrayDeque<>());
        }
        synchronized (queue) {
            if (batch.priority) {
                queue.addFirst(batch);
            } else {
                queue.addLast(batch);
            }
        }
    }

    private static boolean playerWithin(World world, int chunkX, int chunkZ, int chunks) {
        for (EntityPlayer player : world.playerEntities) {
            if (Math.abs((MathHelper.floor(player.posX) >> 4) - chunkX) <= chunks
                    && Math.abs((MathHelper.floor(player.posZ) >> 4) - chunkZ) <= chunks) {
                return true;
            }
        }
        return false;
    }

    private static double nearestPlayerDistSq(World world, long chunkKey) {
        int cx = (int) chunkKey;
        int cz = (int) (chunkKey >> 32);
        double best = Double.MAX_VALUE;
        for (EntityPlayer player : world.playerEntities) {
            double dx = (MathHelper.floor(player.posX) >> 4) - cx;
            double dz = (MathHelper.floor(player.posZ) >> 4) - cz;
            best = Math.min(best, dx * dx + dz * dz);
        }
        return best == Double.MAX_VALUE ? 0.0 : best;
    }

    private static Chunk loadedChunk(World world, long key) {
        return world.getChunkProvider().getLoadedChunk((int) key, (int) (key >> 32));
    }

    private static void relightColumn(Chunk chunk, int key, int y) {
        ((BulkTemplateRelight.RelightAccess) chunk).aqtweaks$relightBlock(key & 15, y, key >> 4);
    }

    /** Runs a whole batch now: all columns, then all remaining checks. */
    private static void flushBatch(World world, Batch batch) {
        for (Iterator<Long2ObjectOpenHashMap.Entry<Int2IntOpenHashMap>> it = batch.columns.long2ObjectEntrySet().iterator();
             it.hasNext(); ) {
            Long2ObjectOpenHashMap.Entry<Int2IntOpenHashMap> e = it.next();
            Chunk chunk = loadedChunk(world, e.getLongKey());
            if (chunk != null) {
                for (Int2IntMap.Entry col : e.getValue().int2IntEntrySet()) {
                    relightColumn(chunk, col.getIntKey(), col.getIntValue());
                }
            }
            it.remove();
        }
        for (; batch.nextCheck < batch.checks.size(); batch.nextCheck++) {
            world.checkLight(BlockPos.fromLong(batch.checks.getLong(batch.nextCheck)));
        }
    }

    /** Runs the pending work of one chunk now (it is about to be saved). */
    private static void flushChunk(World world, Chunk chunk) {
        ArrayDeque<Batch> queue = QUEUES.get(world);
        if (queue == null) return;
        long key = ChunkPos.asLong(chunk.x, chunk.z);
        synchronized (queue) {
            if (queue.isEmpty()) return;
            for (Batch batch : queue) {
                Int2IntOpenHashMap cols = batch.columns.remove(key);
                if (cols != null) {
                    for (Int2IntMap.Entry col : cols.int2IntEntrySet()) {
                        relightColumn(chunk, col.getIntKey(), col.getIntValue());
                    }
                }
                LongArrayList kept = new LongArrayList();
                for (int i = batch.nextCheck; i < batch.checks.size(); i++) {
                    long packed = batch.checks.getLong(i);
                    BlockPos pos = BlockPos.fromLong(packed);
                    if (ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4) == key) {
                        world.checkLight(pos);
                    } else {
                        kept.add(packed);
                    }
                }
                batch.checks = kept;
                batch.nextCheck = 0;
            }
        }
        StageTimer.recordNs("paste.unloadFlush", 0L, "chunk=" + chunk.x + "," + chunk.z);
    }

    private static void flushWorld(World world) {
        ArrayDeque<Batch> queue = QUEUES.remove(world);
        if (queue == null) return;
        synchronized (queue) {
            for (Batch batch : queue) {
                flushBatch(world, batch);
            }
            queue.clear();
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

    /** More work this tick? The floor always allows a few units; after that the slice (or the count cap) decides. */
    private static boolean allow(int units, boolean priority) {
        if (units < FLOOR_UNITS) return true;
        if (!SLICE.clock) return units < (priority ? MAX_UNITS_NO_CLOCK_PRIORITY : MAX_UNITS_NO_CLOCK);
        double limit = priority ? SLICE.nearMs : SLICE.ms;
        if (limit <= 0.0) return false;
        if ((System.nanoTime() - SLICE.startNs) / 1_000_000.0 >= limit) return false;
        return SLICE.fullTps || StewardPacing.remainingMs(40.0) >= 2.0;
    }

    private static void tick(World world) {
        ArrayDeque<Batch> queue = QUEUES.get(world);
        if (queue == null) return;
        synchronized (queue) {
            if (queue.isEmpty()) return;
            beginSlice(world);
            long t0 = System.nanoTime();
            long now = world.getTotalWorldTime();
            int units = 0;
            int maxAge = cfg().deferTemplateRelightMaxAgeTicks;
            while (!queue.isEmpty()) {
                Batch batch = queue.peekFirst();
                if (now - batch.queuedTick > maxAge) {
                    flushBatch(world, batch);
                    StageTimer.recordNs("paste.expired", 0L, "cols=" + batch.totalColumns);
                } else {
                    units += drainBatch(world, batch, units);
                    if (!batchDone(batch)) break;
                    StageTimer.recordNs("paste.latency", 0L, "ticks=" + (now - batch.queuedTick)
                            + ",cols=" + batch.totalColumns + ",checks=" + batch.totalChecks);
                }
                queue.pollFirst();
            }
            if (units > 0) {
                int pending = 0;
                for (Batch b : queue) {
                    for (Int2IntOpenHashMap m : b.columns.values()) {
                        pending += m.size();
                    }
                    pending += b.checks.size() - b.nextCheck;
                }
                StageTimer.recordNs("paste.drain", System.nanoTime() - t0,
                        "units=" + units + ",pending=" + pending);
            }
        }
    }

    private static boolean batchDone(Batch batch) {
        return batch.columns.isEmpty() && batch.nextCheck >= batch.checks.size();
    }

    /** Runs units of one batch while {@link #allow} says so. Returns the units run. */
    private static int drainBatch(World world, Batch batch, int unitsSoFar) {
        int units = 0;
        if (!batch.columns.isEmpty()) {
            long[] keys = batch.columns.keySet().toLongArray();
            if (keys.length > 1) {
                final double[] dist = new double[keys.length];
                Integer[] order = new Integer[keys.length];
                for (int i = 0; i < keys.length; i++) {
                    dist[i] = nearestPlayerDistSq(world, keys[i]);
                    order[i] = i;
                }
                java.util.Arrays.sort(order, (a, b) -> Double.compare(dist[a], dist[b]));
                long[] sorted = new long[keys.length];
                for (int i = 0; i < keys.length; i++) {
                    sorted[i] = keys[order[i]];
                }
                keys = sorted;
            }
            for (long key : keys) {
                Int2IntOpenHashMap cols = batch.columns.get(key);
                if (cols == null) continue;
                Chunk chunk = loadedChunk(world, key);
                if (chunk == null) {
                    batch.columns.remove(key);
                    continue;
                }
                for (Iterator<Int2IntMap.Entry> it = cols.int2IntEntrySet().iterator(); it.hasNext(); ) {
                    if (!allow(unitsSoFar + units, batch.priority)) return units;
                    Int2IntMap.Entry col = it.next();
                    relightColumn(chunk, col.getIntKey(), col.getIntValue());
                    it.remove();
                    units++;
                }
                batch.columns.remove(key);
            }
        }
        // Phase 2 only once every column of the batch is done, so light checks read final height maps.
        if (batch.columns.isEmpty()) {
            while (batch.nextCheck < batch.checks.size()) {
                if (!allow(unitsSoFar + units, batch.priority)) return units;
                world.checkLight(BlockPos.fromLong(batch.checks.getLong(batch.nextCheck++)));
                units++;
            }
        }
        return units;
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
