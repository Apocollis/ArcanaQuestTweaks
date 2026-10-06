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
    private static final int EXAMINE_PER_TICK = 64;
    private static final long MAX_AGE_TICKS = 6000L;

    /** Position (packed long) to the world tick it was first queued. Insertion order = retry order. */
    private static final Map<World, Long2LongLinkedOpenHashMap> QUEUES =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final ThreadLocal<Boolean> DRAINING = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private DeferredRelight() {}

    public static void onCheckFailed(World world, BlockPos pos) {
        if (world.isRemote || DRAINING.get() || !ArcanaQuestTweaksConfig.RtgModuleConfig.surface.enableDeferredRelight) {
            return;
        }
        // Only light emitters. The check runs for every block that changes how much light passes, so a shrine
        // or village queues thousands of plain solid blocks; that overflowed the queue (dropping the lanterns
        // it exists for) and made every retry a full flood-fill. Blocks that only block light need nothing:
        // the emitter's own check, run later with everything loaded, accounts for them. Never read a chunk
        // that is not loaded (getBlockState would generate it).
        if (!world.isBlockLoaded(pos) || world.getBlockState(pos).getLightValue(world, pos) <= 0) {
            return;
        }
        Long2LongLinkedOpenHashMap queue = QUEUES.computeIfAbsent(world, w -> new Long2LongLinkedOpenHashMap());
        synchronized (queue) {
            long key = pos.toLong();
            if (!queue.containsKey(key)) {
                queue.put(key, world.getTotalWorldTime());
            }
            while (queue.size() > MAX_QUEUE) {
                queue.removeFirstLong();
            }
        }
    }

    private static void tick(World world) {
        Long2LongLinkedOpenHashMap queue = QUEUES.get(world);
        if (queue == null) {
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
                for (int i = 0; i < n; i++) {
                    long key = queue.firstLongKey();
                    long created = queue.removeFirstLong();
                    if (now - created > MAX_AGE_TICKS) {
                        continue;
                    }
                    BlockPos pos = BlockPos.fromLong(key);
                    if (world.isAreaLoaded(pos, 17, false)) {
                        world.checkLight(pos);
                    } else {
                        queue.put(key, created);
                    }
                }
            } finally {
                DRAINING.set(Boolean.FALSE);
            }
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
