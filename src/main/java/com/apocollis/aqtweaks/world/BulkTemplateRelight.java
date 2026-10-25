package com.apocollis.aqtweaks.world;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.perf.StageTimer;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * One relight per column for big template pastes. {@code Template.addBlocksToWorld} calls
 * {@code World.setBlockState} for every block, and each call runs {@code Chunk.relightBlock} plus
 * {@code World.checkLight}. A Dungeon Additions High City paste spent 22.6 of 26 s there.
 *
 * <p>While a qualifying paste runs on a thread, those two calls are recorded instead of run (the highest
 * Y per column, and each checked position). When the paste returns they are replayed once, relight first
 * so the height map is final before any light flood fill reads it. {@code relightBlock} recomputes the
 * column height from the blocks, so a single call with the highest touched Y gives the same height map
 * as the per-block calls. The state is thread-local, server-side only, and flushed if a game tick passes
 * (a paste that threw never reached {@link #end}).
 */
public final class BulkTemplateRelight {

    /** Mixed into {@code Chunk} so the replay can reach the private {@code relightBlock}. */
    public interface RelightAccess {
        void aqtweaks$relightBlock(int x, int y, int z);
    }

    private static final class State {
        final World world;
        final long worldTime;
        final boolean bulk;
        final long startNs;
        final String size;
        int depth = 1;
        final Map<Chunk, Int2IntOpenHashMap> columns = new IdentityHashMap<>();
        final LongOpenHashSet checkSet = new LongOpenHashSet();
        final LongArrayList checkOrder = new LongArrayList();
        int relightCalls;
        int checkCalls;

        State(World world, boolean bulk, long startNs, String size) {
            this.world = world;
            this.worldTime = world.getTotalWorldTime();
            this.bulk = bulk;
            this.startNs = startNs;
            this.size = size;
        }
    }

    private static final ThreadLocal<State> STATE = new ThreadLocal<>();

    private BulkTemplateRelight() {}

    /** Called at the head of {@code Template.addBlocksToWorld}. */
    public static void begin(World world, int sizeX, int sizeY, int sizeZ) {
        State existing = STATE.get();
        if (existing != null) {
            if (existing.world == world && existing.worldTime == world.getTotalWorldTime()) {
                existing.depth++;
                return;
            }
            STATE.remove();
            flush(existing);
        }
        if (world == null || world.isRemote) return;
        ArcanaQuestTweaksConfig.DepthsModuleConfig.General cfg = ArcanaQuestTweaksConfig.DepthsModuleConfig.general;
        long volume = (long) sizeX * sizeY * sizeZ;
        boolean bulk = cfg.bulkTemplateRelight && volume >= cfg.bulkTemplateRelightMinVolume;
        long start = StageTimer.start();
        if (!bulk && start == 0L) return;
        STATE.set(new State(world, bulk, start, sizeX + "x" + sizeY + "x" + sizeZ));
    }

    /** Called at the return of {@code Template.addBlocksToWorld}. */
    public static void end() {
        State s = STATE.get();
        if (s == null) return;
        if (--s.depth > 0) return;
        STATE.remove();
        if (s.bulk && ArcanaQuestTweaksConfig.DepthsModuleConfig.general.deferTemplateRelight) {
            PasteRelightQueue.enqueue(s.world, s.columns, s.checkOrder);
            StageTimer.end("paste.template", s.startNs, s.size + ",bulk,deferred");
            return;
        }
        long replayStart = StageTimer.start();
        int cols = flush(s);
        if (s.bulk) {
            StageTimer.end("paste.replay", replayStart,
                    "cols=" + cols + ",checks=" + s.checkOrder.size() + ",relights=" + s.relightCalls);
        }
        StageTimer.end("paste.template", s.startNs, s.size + (s.bulk ? ",bulk" : ",plain"));
    }

    /** Chunk hook. Returns true when the relight was recorded and the real call must be skipped. */
    public static boolean onRelight(Chunk chunk, int x, int y, int z) {
        State s = STATE.get();
        if (s == null || !s.bulk) {
            StageTimer.recordNs("relight.calls", 0L, null);
            return false;
        }
        if (stale(s)) return false;
        s.relightCalls++;
        s.columns.computeIfAbsent(chunk, c -> {
            Int2IntOpenHashMap m = new Int2IntOpenHashMap();
            m.defaultReturnValue(Integer.MIN_VALUE);
            return m;
        }).merge(x | (z << 4), y, Math::max);
        return true;
    }

    /** World hook. Returns true when the check was recorded and the real call must be skipped. */
    public static boolean onCheck(World world, BlockPos pos) {
        State s = STATE.get();
        if (s == null || !s.bulk || s.world != world || stale(s)) return false;
        s.checkCalls++;
        if (s.checkSet.add(pos.toLong())) {
            s.checkOrder.add(pos.toLong());
        }
        return true;
    }

    private static boolean stale(State s) {
        return s.worldTime != s.world.getTotalWorldTime();
    }

    /** Replays recorded work. Returns the number of columns relit. */
    private static int flush(State s) {
        int cols = 0;
        for (Map.Entry<Chunk, Int2IntOpenHashMap> e : s.columns.entrySet()) {
            RelightAccess access = (RelightAccess) e.getKey();
            for (Int2IntOpenHashMap.Entry col : e.getValue().int2IntEntrySet()) {
                int key = col.getIntKey();
                access.aqtweaks$relightBlock(key & 15, col.getIntValue(), key >> 4);
                cols++;
            }
        }
        s.columns.clear();
        for (int i = 0; i < s.checkOrder.size(); i++) {
            s.world.checkLight(BlockPos.fromLong(s.checkOrder.getLong(i)));
        }
        s.checkOrder.clear();
        s.checkSet.clear();
        return cols;
    }
}
