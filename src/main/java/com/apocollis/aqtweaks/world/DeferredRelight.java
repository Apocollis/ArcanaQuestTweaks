package com.apocollis.aqtweaks.world;

import com.apocollis.aqtweaks.ArcanaQuestTweaks;
import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import it.unimi.dsi.fastutil.longs.Long2LongLinkedOpenHashMap;
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
    }

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
            if (s.immediate + s.queued + s.retried + s.expired + s.overflow > 0) {
                org.apache.logging.log4j.LogManager.getLogger("AQTweaks-Relight").info(
                        "[AQ-RELIGHT] dim={} last 60s: lit at placement={} queued={} retried={} expired unlit={} overflow={} pending={} | queued at:{} | expired at:{}",
                        world.provider.getDimension(), s.immediate, s.queued, s.retried, s.expired, s.overflow, pending,
                        s.queuedSample, s.expiredSample);
            }
            s.immediate = 0;
            s.queued = 0;
            s.retried = 0;
            s.expired = 0;
            s.overflow = 0;
            s.queuedSamples = 0;
            s.expiredSamples = 0;
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
                    } else if (checks < CHECKS_PER_TICK && world.isAreaLoaded(pos, 17, false)) {
                        world.checkLight(pos);
                        checks++;
                        countRetried(world);
                    } else {
                        queue.put(key, created);
                    }
                }
                // Distant emitters only use the budget the nearby ones left over.
                for (int i = 0; i < farCount; i++) {
                    BlockPos pos = BlockPos.fromLong(farKeys[i]);
                    if (checks < CHECKS_PER_TICK && world.isAreaLoaded(pos, 17, false)) {
                        world.checkLight(pos);
                        checks++;
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
