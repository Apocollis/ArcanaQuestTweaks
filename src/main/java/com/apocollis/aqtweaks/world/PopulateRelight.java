package com.apocollis.aqtweaks.world;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.perf.StageTimer;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Takes the per-block light walks out of a server-thread {@code Chunk.populate(IChunkGenerator)} (plan
 * {@code 20261027-2218-populate-column-relight.md}). While a populate runs, {@code Chunk.relightBlock} and
 * {@code World.checkLight} are recorded (the highest Y per column, and each checked position) instead of run, and the
 * opening {@code Chunk.checkLight} only marks the chunk terrain-populated. When the populate returns, each touched
 * chunk's {@link Record} goes to {@link PopulateRelightQueue}, which replays one {@code relightBlock} per dirty column,
 * then the checks, then the real {@code Chunk.checkLight}, over later ticks. A chunk with a record is held: its
 * {@code isLightPopulated} stays false, so vanilla does not send it, and {@code Chunk.checkLight} is cancelled for it.
 *
 * <p>Everything runs on the server thread. The scope is thread-local, depth counted and flushed if a game tick passes
 * (a populate that threw never reached {@link #end}). A template paste keeps {@link BulkTemplateRelight}'s recording;
 * the populate recorder only takes the calls that recorder declines.
 *
 * <p>Reading a height from a column with a pending record replays that one column first, so decorators that run
 * later in the same populate (snow, ice, {@code World.getHeight} placements) see final heights.
 */
public final class PopulateRelight {

    /** Mixed into {@code Chunk}: the pending record, and the flag the cancelled opening check has to set. */
    public interface Pending {
        Record aqtweaks$pending();

        void aqtweaks$setPending(Record record);

        void aqtweaks$markTerrainPopulated();

        void aqtweaks$relightBlockReplay(int x, int y, int z);
    }

    /** What one chunk still owes: dirty columns, light checks, and whether the opening full check is owed. */
    public static final class Record {
        final Chunk chunk;
        final Int2IntOpenHashMap columns = new Int2IntOpenHashMap();
        final LongArrayList checks = new LongArrayList();
        final LongOpenHashSet checkSet = new LongOpenHashSet();
        boolean needsCheck;
        boolean queued;
        long queuedTick;

        Record(Chunk chunk) {
            this.chunk = chunk;
            this.columns.defaultReturnValue(Integer.MIN_VALUE);
        }
    }

    private static final class Scope {
        final World world;
        final long worldTime;
        final ArrayDeque<Chunk> populating = new ArrayDeque<>();
        final Map<Chunk, Record> records = new IdentityHashMap<>();

        Scope(World world) {
            this.world = world;
            this.worldTime = world.getTotalWorldTime();
        }
    }

    private static final ThreadLocal<Scope> SCOPE = new ThreadLocal<>();
    /** Greater than zero while a replay runs on this thread, so its calls are not recorded again. */
    private static final ThreadLocal<int[]> REPLAY = ThreadLocal.withInitial(() -> new int[1]);
    private static final int MAX_NESTED_HEIGHT_REPLAYS = 4;

    private PopulateRelight() {}

    private static ArcanaQuestTweaksConfig.DepthsModuleConfig.General cfg() {
        return ArcanaQuestTweaksConfig.DepthsModuleConfig.general;
    }

    public static boolean enabled() {
        return cfg().populateRelight;
    }

    private static boolean serverThread(World world) {
        return world instanceof WorldServer && ((WorldServer) world).isCallingFromMinecraftThread();
    }

    // --- scope -----------------------------------------------------------------------------------------------

    /** Head of RTG-Plus {@code LatePopulatingChunk.doPopulate}: the same scope as vanilla populate. */
    public static void beginRtg(World world, Chunk chunk) {
        if (!cfg().populateRelightRtg) return;
        begin(world, chunk);
    }

    /**
     * Chunk Steward staged populate: the decorations and the rest of this chunk's populate run after the populate call
     * returned, with the chunk already marked terrain populated. Opens the same scope for that part, so its block
     * writes and light checks are recorded too. Pair with {@link #end}.
     */
    public static void beginResume(World world, Chunk chunk) {
        if (world == null || chunk == null || world.isRemote || !enabled() || !serverThread(world)) return;
        open(world, chunk);
    }

    /** Head of {@code Chunk.populate(IChunkGenerator)}. */
    public static void begin(World world, Chunk chunk) {
        if (world == null || chunk == null || world.isRemote || !enabled()) return;
        // A populate of an already populated chunk only generates structures; no light work to take out.
        if (chunk.isTerrainPopulated() || !serverThread(world)) return;
        open(world, chunk);
    }

    private static void open(World world, Chunk chunk) {
        Scope s = SCOPE.get();
        if (s != null) {
            if (s.world == world && s.worldTime == world.getTotalWorldTime()) {
                s.populating.push(chunk);
                return;
            }
            SCOPE.remove();
            finish(s);
        }
        s = new Scope(world);
        s.populating.push(chunk);
        SCOPE.set(s);
    }

    /** Return of {@code Chunk.populate(IChunkGenerator)}. */
    public static void end(Chunk chunk) {
        Scope s = SCOPE.get();
        if (s == null || s.populating.isEmpty() || s.populating.peek() != chunk) return;
        s.populating.pop();
        if (!s.populating.isEmpty()) return;
        SCOPE.remove();
        finish(s);
    }

    private static void finish(Scope s) {
        int cols = 0;
        int checks = 0;
        for (Record record : s.records.values()) {
            cols += record.columns.size();
            checks += record.checks.size();
        }
        if (!s.records.isEmpty()) {
            StageTimer.recordNs("populate.recorded", 0L,
                    "chunks=" + s.records.size() + ",cols=" + cols + ",checks=" + checks);
        }
        for (Record record : new ArrayList<>(s.records.values())) {
            PopulateRelightQueue.take(s.world, record);
        }
    }

    private static boolean stale(Scope s) {
        return s.worldTime != s.world.getTotalWorldTime();
    }

    private static Record recordFor(Scope s, Chunk chunk) {
        Pending pending = (Pending) chunk;
        Record record = pending.aqtweaks$pending();
        if (record == null) {
            record = new Record(chunk);
            pending.aqtweaks$setPending(record);
            s.records.put(chunk, record);
        }
        return record;
    }

    // --- recording hooks --------------------------------------------------------------------------------------

    /** Chunk hook. True when the relight was recorded and the real call must be skipped. */
    public static boolean onRelight(Chunk chunk, int x, int y, int z) {
        Scope s = SCOPE.get();
        if (s == null || REPLAY.get()[0] > 0 || stale(s)) return false;
        if (chunk.getWorld() != s.world) return false;
        recordFor(s, chunk).columns.merge(x | (z << 4), y, Math::max);
        return true;
    }

    /** World hook. True when the check was recorded and the real call must be skipped. */
    public static boolean onCheck(World world, BlockPos pos) {
        Scope s = SCOPE.get();
        if (s == null || REPLAY.get()[0] > 0 || s.world != world || stale(s)) return false;
        Chunk chunk = world.getChunkProvider().getLoadedChunk(pos.getX() >> 4, pos.getZ() >> 4);
        if (chunk == null) return false;
        Record record = recordFor(s, chunk);
        if (record.checkSet.add(pos.toLong())) {
            record.checks.add(pos.toLong());
        }
        return true;
    }

    /**
     * Head of {@code Chunk.checkLight()}. True when the call must be skipped: a held chunk must not walk 256
     * columns on a stale height map and set light-populated early, and the populate's own opening check only marks the
     * chunk terrain-populated (the real check is replayed from the queue).
     */
    public static boolean onCheckLight(Chunk chunk) {
        Pending pending = (Pending) chunk;
        Record held = pending.aqtweaks$pending();
        if (held != null) {
            // A neighbour's populate touched this chunk first, so it already owes a record. Its own populate's opening
            // check still has to mark it terrain populated and still owes the real check, or the replay never sets the
            // flags: the chunk stays unpopulated, is never sent, and RTG-Plus populates it again every time it is asked.
            Scope own = SCOPE.get();
            if (own != null && REPLAY.get()[0] == 0 && !stale(own) && own.populating.peek() == chunk
                    && !chunk.isTerrainPopulated()) {
                held.needsCheck = true;
                pending.aqtweaks$markTerrainPopulated();
            }
            return true;
        }
        Scope s = SCOPE.get();
        if (s == null || REPLAY.get()[0] > 0 || stale(s)) return false;
        if (s.populating.peek() != chunk || chunk.isTerrainPopulated()) return false;
        recordFor(s, chunk).needsCheck = true;
        pending.aqtweaks$markTerrainPopulated();
        return true;
    }

    /** Height getters: replay the one column when it has a pending record, so the read sees the final height. */
    public static void onHeightRead(Chunk chunk, int x, int z) {
        Pending pending = (Pending) chunk;
        Record record = pending.aqtweaks$pending();
        if (record == null) return;
        int key = (x & 15) | ((z & 15) << 4);
        int y = record.columns.remove(key);
        if (y == Integer.MIN_VALUE) return;
        int[] depth = REPLAY.get();
        if (depth[0] > MAX_NESTED_HEIGHT_REPLAYS) {
            record.columns.put(key, y);
            return;
        }
        depth[0]++;
        try {
            pending.aqtweaks$relightBlockReplay(x & 15, y, z & 15);
        } finally {
            depth[0]--;
        }
        StageTimer.recordNs("populate.heightReplay", 0L, null);
    }

    // --- replay ----------------------------------------------------------------------------------------------

    /** Runs everything a record still owes, in the order the vanilla calls would have run. Server thread. */
    static void replay(Record record) {
        Chunk chunk = record.chunk;
        Pending pending = (Pending) chunk;
        if (pending.aqtweaks$pending() == record) {
            pending.aqtweaks$setPending(null);
        }
        record.queued = false;
        World world = chunk.getWorld();
        int[] depth = REPLAY.get();
        depth[0]++;
        try {
            for (Int2IntOpenHashMap.Entry col : record.columns.int2IntEntrySet()) {
                int key = col.getIntKey();
                pending.aqtweaks$relightBlockReplay(key & 15, col.getIntValue(), key >> 4);
            }
            record.columns.clear();
            for (int i = 0; i < record.checks.size(); i++) {
                world.checkLight(BlockPos.fromLong(record.checks.getLong(i)));
            }
            record.checks.clear();
            record.checkSet.clear();
            if (record.needsCheck) {
                record.needsCheck = false;
                chunk.checkLight();
            }
        } finally {
            depth[0]--;
        }
    }

    // --- queries ----------------------------------------------------------------------------------------------

    /** True when this chunk holds a record that has not been replayed. */
    public static boolean isPending(Chunk chunk) {
        return chunk != null && ((Pending) chunk).aqtweaks$pending() != null;
    }

    /** True when the chunk or any of its eight neighbours is waiting for its light replay (loaded chunks only). */
    public static boolean pendingNear(World world, int cx, int cz) {
        if (world == null || world.isRemote) return false;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (isPending(world.getChunkProvider().getLoadedChunk(cx + dx, cz + dz))) return true;
            }
        }
        return false;
    }
}
