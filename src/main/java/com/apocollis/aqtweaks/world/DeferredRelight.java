package com.apocollis.aqtweaks.world;

import com.apocollis.aqtweaks.ArcanaQuestTweaks;
import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.pacing.StewardPacing;
import it.unimi.dsi.fastutil.longs.Long2LongLinkedOpenHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Light checks that were skipped because the surrounding chunks did not exist yet, retried later.
 *
 * <p>{@code World.checkLight} does nothing and returns {@code false} unless the 17-block area around
 * the position is loaded. Structures placed while a chunk generates (villages, monuments, shrines)
 * routinely place torches and lanterns next to chunks that are not generated yet, so those emitters
 * never flood light and stay dark until something else triggers a check. {@code MixinWorldCheckLight}
 * reports every failed check here; each world's queue is retried on world-tick end once the area is
 * loaded. Only light-emitting positions are queued. Bounded by size and age so fast exploration cannot grow it.
 */
public final class DeferredRelight {

    private static final int MAX_QUEUE = 8192;
    private static final int EXAMINE_PER_TICK = 128;
    private static final int CHECKS_PER_TICK = 48;
    private static final int NEAR_FLOOR_PER_TICK = 4;
    /** Emitters within this many blocks (horizontal) of a player are retried before distant ones. */
    private static final double NEAR_SQ = 96.0 * 96.0;
    private static final long MAX_AGE_TICKS = 6000L;

    /** Position (packed long) to the world tick it was first queued. Insertion order = retry order. */
    private static final Map<World, Long2LongLinkedOpenHashMap> QUEUES =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final ThreadLocal<Boolean> DRAINING = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private DeferredRelight() {}

    /** Per-world counters for {@code Deferred Relight Debug}; guarded by their own lock. */
    private static final class Stats {
        int immediate;
        int queued;
        int retried;
        int expired;
        int overflow;
        long nextLog;
        final StringBuilder queuedSample = new StringBuilder();
        final StringBuilder expiredSample = new StringBuilder();
        int queuedSamples;
        int expiredSamples;
        int activeTicks;
        int fullTpsTicks;
        double sliceMsSum;
    }

    /** One relight slice per server tick. Later dimensions reuse the start time, not a fresh 8 ms. */
    private static final class SliceClock {
        int tick = Integer.MIN_VALUE;
        boolean clock;
        boolean fullTps;
        double ms;
        long startNs;
    }

    private static final SliceClock SLICE = new SliceClock();

    private static final Map<World, Stats> STATS = Collections.synchronizedMap(new WeakHashMap<>());

    public static boolean debugOn() {
        return ArcanaQuestTweaksConfig.RtgModuleConfig.surface.deferredRelightDebug;
    }

    private static Stats stats(World world) {
        return STATS.computeIfAbsent(world, w -> new Stats());
    }

    private static void sample(StringBuilder sb, int count, BlockPos pos) {
        if (count < 4) {
            sb.append(' ').append(pos.getX()).append(',').append(pos.getY()).append(',').append(pos.getZ());
        }
    }

    /** A light-emitting block that is not a liquid. Lava lakes are the bulk of the load and are not what goes dark visibly. */
    private static boolean isRelevantEmitter(World world, BlockPos pos) {
        net.minecraft.block.state.IBlockState state = world.getBlockState(pos);
        return state.getLightValue(world, pos) > 0 && !state.getMaterial().isLiquid();
    }

    private static boolean nearPlayer(World world, BlockPos pos) {
        if (world.playerEntities.isEmpty()) {
            return true;
        }
        double x = pos.getX() + 0.5;
        double z = pos.getZ() + 0.5;
        for (net.minecraft.entity.player.EntityPlayer player : world.playerEntities) {
            double dx = player.posX - x;
            double dz = player.posZ - z;
            if (dx * dx + dz * dz <= NEAR_SQ) {
                return true;
            }
        }
        return false;
    }

    /** Debug only: an emitter whose light check succeeded at placement. */
    public static void onCheckSucceeded(World world, BlockPos pos) {
        if (world.isRemote || DRAINING.get() || !world.isBlockLoaded(pos) || !isRelevantEmitter(world, pos)) {
            return;
        }
        Stats s = stats(world);
        synchronized (s) {
            s.immediate++;
        }
    }

    public static void onCheckFailed(World world, BlockPos pos) {
        if (world.isRemote || DRAINING.get() || !ArcanaQuestTweaksConfig.RtgModuleConfig.surface.enableDeferredRelight) {
            return;
        }
        // Only light emitters. The check runs for every block that changes how much light passes, so a shrine
        // or village queues thousands of plain solid blocks; that overflowed the queue (dropping the lanterns
        // it exists for) and made every retry a full flood-fill. Blocks that only block light need nothing:
        // the emitter's own check, run later with everything loaded, accounts for them. Never read a chunk
        // that is not loaded (getBlockState would generate it).
        if (!world.isBlockLoaded(pos) || !isRelevantEmitter(world, pos)) {
            return;
        }
        boolean debug = debugOn();
        Long2LongLinkedOpenHashMap queue = QUEUES.computeIfAbsent(world, w -> new Long2LongLinkedOpenHashMap());
        synchronized (queue) {
            long key = pos.toLong();
            if (!queue.containsKey(key)) {
                queue.put(key, world.getTotalWorldTime());
                if (debug) {
                    Stats s = stats(world);
                    synchronized (s) {
                        sample(s.queuedSample, s.queuedSamples, pos);
                        s.queuedSamples++;
                        s.queued++;
                    }
                }
            }
            while (queue.size() > MAX_QUEUE) {
                queue.removeFirstLong();
                if (debug) {
                    Stats s = stats(world);
                    synchronized (s) {
                        s.overflow++;
                    }
                }
            }
        }
    }

    private static void logStats(World world, long now, int pending) {
        Stats s = stats(world);
        synchronized (s) {
            if (s.nextLog == 0L) {
                s.nextLog = now + 1200L;
                return;
            }
            if (now < s.nextLog) {
                return;
            }
            s.nextLog = now + 1200L;
            if (s.immediate + s.queued + s.retried + s.expired + s.overflow + s.activeTicks > 0) {
                double avgSlice = s.activeTicks == 0 ? 0.0 : s.sliceMsSum / s.activeTicks;
                org.apache.logging.log4j.LogManager.getLogger("AQTweaks-Relight").info(
                        "[AQ-RELIGHT] dim={} last 60s: lit at placement={} queued={} retried={} expired unlit={} overflow={} pending={} ticks={} fullTpsTicks={} avgSliceMs={} | queued at:{} | expired at:{}",
                        world.provider.getDimension(), s.immediate, s.queued, s.retried, s.expired, s.overflow, pending,
                        s.activeTicks, s.fullTpsTicks, String.format(java.util.Locale.ROOT, "%.1f", avgSlice),
                        s.queuedSample, s.expiredSample);
            }
            s.immediate = 0;
            s.queued = 0;
            s.retried = 0;
            s.expired = 0;
            s.overflow = 0;
            s.queuedSamples = 0;
            s.expiredSamples = 0;
            s.activeTicks = 0;
            s.fullTpsTicks = 0;
            s.sliceMsSum = 0.0;
            s.queuedSample.setLength(0);
            s.expiredSample.setLength(0);
        }
    }

    private static void countRetried(World world) {
        if (debugOn()) {
            Stats s = stats(world);
            synchronized (s) {
                s.retried++;
            }
        }
    }

    /**
     * Starts the shared slice on the first non-empty queue of this server tick.
     * A negative {@link StewardPacing#sliceBudgetMs} leaves the clock off (count caps only).
     */
    private static void beginSlice(World world) {
        MinecraftServer server = world.getMinecraftServer();
        int tick = server == null ? SLICE.tick : server.getTickCounter();
        if (server != null && SLICE.tick == tick) {
            return;
        }
        double maxSlice = ArcanaQuestTweaksConfig.RtgModuleConfig.surface.deferredRelightMaxSliceMs;
        double budget = StewardPacing.sliceBudgetMs(maxSlice, 0.5);
        SLICE.tick = tick;
        if (budget < 0.0) {
            SLICE.clock = false;
            SLICE.fullTps = false;
            SLICE.ms = 0.0;
            SLICE.startNs = 0L;
            return;
        }
        SLICE.clock = true;
        SLICE.fullTps = StewardPacing.getLoadFactor() >= 1.0;
        SLICE.ms = budget;
        SLICE.startNs = com.apocollis.aqtweaks.pacing.TickSlice.start(server);
    }

    private static void noteSlice(World world) {
        if (!debugOn()) {
            return;
        }
        Stats s = stats(world);
        synchronized (s) {
            s.activeTicks++;
            if (SLICE.clock) {
                s.sliceMsSum += SLICE.ms;
                if (SLICE.fullTps) {
                    s.fullTpsTicks++;
                }
            }
        }
    }

    /** After the near floor, false when the count cap is hit, the slice is empty, or its time is gone. */
    private static boolean withinSlice(int checks, int floor, long sliceStartNs, double sliceMs, boolean fullTps) {
        if (checks < floor) {
            return true;
        }
        if (checks >= CHECKS_PER_TICK || sliceMs <= 0.0) {
            return false;
        }
        if ((System.nanoTime() - sliceStartNs) / 1_000_000.0 >= sliceMs) {
            return false;
        }
        return fullTps || StewardPacing.remainingMs(40.0) >= 2.0;
    }

    private static boolean canRunNearCheck(int checks) {
        if (checks >= CHECKS_PER_TICK) {
            return false;
        }
        if (!SLICE.clock) {
            return true;
        }
        return withinSlice(checks, NEAR_FLOOR_PER_TICK, SLICE.startNs, SLICE.ms, SLICE.fullTps);
    }

    private static void tick(World world) {
        Long2LongLinkedOpenHashMap queue = QUEUES.get(world);
        if (queue == null) {
            if (debugOn()) {
                logStats(world, world.getTotalWorldTime(), 0);
            }
            return;
        }
        long now = world.getTotalWorldTime();
        synchronized (queue) {
            int n = Math.min(EXAMINE_PER_TICK, queue.size());
            if (n == 0) {
                return;
            }
            beginSlice(world);
            noteSlice(world);
            DRAINING.set(Boolean.TRUE);
            try {
                int checks = 0;
                long[] farKeys = new long[n];
                long[] farCreated = new long[n];
                int farCount = 0;
                for (int i = 0; i < n; i++) {
                    long key = queue.firstLongKey();
                    long created = queue.removeFirstLong();
                    BlockPos pos = BlockPos.fromLong(key);
                    if (now - created > MAX_AGE_TICKS) {
                        if (debugOn()) {
                            Stats s = stats(world);
                            synchronized (s) {
                                sample(s.expiredSample, s.expiredSamples, pos);
                                s.expiredSamples++;
                                s.expired++;
                            }
                        }
                        continue;
                    }
                    if (!nearPlayer(world, pos)) {
                        farKeys[farCount] = key;
                        farCreated[farCount++] = created;
                    } else if (canRunNearCheck(checks) && world.isAreaLoaded(pos, 17, false)) {
                        world.checkLight(pos);
                        checks++;
                        countRetried(world);
                    } else {
                        queue.put(key, created);
                    }
                }
                // Distant emitters use leftover slice time. The 1/20 floor is outside withinSlice:
                // near work runs first and must not block far torches for the whole exploration.
                // When the slice clock is off, leftover count caps apply and that extra floor does not.
                boolean paused = StewardPacing.shouldPauseOptionalWork();
                boolean floorTick = Math.floorMod(now, 20L) == 0;
                int farChecks = 0;
                for (int i = 0; i < farCount; i++) {
                    BlockPos pos = BlockPos.fromLong(farKeys[i]);
                    boolean inSlice = SLICE.clock && !paused
                            && withinSlice(checks, 0, SLICE.startNs, SLICE.ms, SLICE.fullTps);
                    boolean unpaced = !SLICE.clock && !paused && checks < CHECKS_PER_TICK;
                    boolean sliceSpent = SLICE.clock && !paused
                            && !withinSlice(checks, 0, SLICE.startNs, SLICE.ms, SLICE.fullTps);
                    boolean floor = floorTick && farChecks < 1 && checks < CHECKS_PER_TICK && (paused || sliceSpent);
                    if ((inSlice || unpaced || floor) && world.isAreaLoaded(pos, 17, false)) {
                        world.checkLight(pos);
                        checks++;
                        farChecks++;
                        countRetried(world);
                    } else {
                        queue.put(farKeys[i], farCreated[i]);
                    }
                }
            } finally {
                DRAINING.set(Boolean.FALSE);
            }
        }
        if (debugOn()) {
            logStats(world, now, queue.size());
        }
    }

    @Mod.EventBusSubscriber(modid = ArcanaQuestTweaks.MODID)
    public static final class Events {
        @SubscribeEvent
        public static void onWorldTick(TickEvent.WorldTickEvent event) {
            if (event.phase != TickEvent.Phase.END || event.world == null || event.world.isRemote) {
                return;
            }
            tick(event.world);
        }

        @SubscribeEvent
        public static void onWorldUnload(WorldEvent.Unload event) {
            if (event.getWorld() != null) {
                QUEUES.remove(event.getWorld());
            }
        }
    }
}
