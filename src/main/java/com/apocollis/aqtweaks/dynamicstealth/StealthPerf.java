package com.apocollis.aqtweaks.dynamicstealth;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Server cost trimming for Dynamic Stealth. Nothing here changes a range, a threshold or a formula.
 *
 * <ul>
 *   <li><b>Ray chunk cache:</b> {@code ImprovedRayTracing.rayTraceBlocks} calls {@code World.getBlockState} for
 *       every block along a ray, and most steps stay in the same chunk. Within one trace the last chunk is kept,
 *       so the per-step {@code World.getChunk} lookup goes away. Same result as {@code World.getBlockState}.</li>
 *   <li><b>Tracker sight memo:</b> {@code DSEntityTrackerEntry.func_180233_c} calls {@code Sight.canSee} for every
 *       tracked entity and player every tick. The answer is reused for a few ticks while neither moved more than
 *       a block. Only that tracker call is memoised; the AI and the stealth gauge still use the real
 *       {@code Sight.canSee}. Effect: an entity can appear or disappear on the client up to the memo ticks late.</li>
 *   <li><b>Visibility packet memo:</b> {@code Network.VisibilityPacket} recomputes {@code Sight.seenEntities} for
 *       the player every tick. The map is reused for a few ticks.</li>
 * </ul>
 */
public final class StealthPerf {

    private static final class Memo {
        long tick;
        boolean seen;
        UUID target;
        int dim;
        double vx, vy, vz, tx, ty, tz;
    }

    private static final class SeenMemo {
        long tick;
        int dim;
        LinkedHashMap<Entity, Double> map;
    }

    private static final Map<EntityPlayerMP, Int2ObjectOpenHashMap<Memo>> TRACKER = new WeakHashMap<>();
    private static final Map<EntityPlayerMP, SeenMemo> SEEN = new WeakHashMap<>();
    private static long nextPrune;

    private static final class RayState {
        World world;
        int cx;
        int cz;
        Chunk chunk;
    }

    private static final ThreadLocal<RayState> RAY = ThreadLocal.withInitial(RayState::new);

    private StealthPerf() {}

    private static ArcanaQuestTweaksConfig.DynamicStealthPerfGeneral cfg() {
        return ArcanaQuestTweaksConfig.DynamicStealthPerfConfig.general;
    }

    // --- ray chunk cache ---

    public static void rayBegin() {
        RayState s = RAY.get();
        s.world = null;
        s.chunk = null;
    }

    public static void rayEnd() {
        RayState s = RAY.get();
        s.world = null;
        s.chunk = null;
    }

    /** Same answer as {@code World.getBlockState}, with the chunk reused across steps of one trace. */
    public static IBlockState rayState(World world, BlockPos pos) {
        if (!cfg().rayChunkCache) {
            return world.getBlockState(pos);
        }
        if (world.isOutsideBuildHeight(pos)) {
            return Blocks.AIR.getDefaultState();
        }
        RayState s = RAY.get();
        int cx = pos.getX() >> 4;
        int cz = pos.getZ() >> 4;
        if (s.chunk == null || s.world != world || s.cx != cx || s.cz != cz) {
            s.world = world;
            s.cx = cx;
            s.cz = cz;
            s.chunk = world.getChunk(cx, cz);
        }
        return s.chunk.getBlockState(pos);
    }

    // --- tracker sight memo ---

    private static boolean memoOn(int ticks) {
        return ticks > 0;
    }

    /** TRUE or FALSE when a still-valid memo exists, null when {@code Sight.canSee} must run. */
    public static Boolean trackerLookup(EntityPlayerMP viewer, Entity target) {
        int ticks = cfg().trackerSightMemoTicks;
        if (!memoOn(ticks)) return null;
        Int2ObjectOpenHashMap<Memo> byTarget = TRACKER.get(viewer);
        if (byTarget == null) return null;
        Memo m = byTarget.get(target.getEntityId());
        if (m == null) return null;
        long now = viewer.world.getTotalWorldTime();
        long age = now - m.tick;
        if (age < 0 || age >= ticks || m.dim != viewer.dimension || !target.getUniqueID().equals(m.target)) return null;
        double dv = sq(viewer.posX - m.vx) + sq(viewer.posY - m.vy) + sq(viewer.posZ - m.vz);
        double dt = sq(target.posX - m.tx) + sq(target.posY - m.ty) + sq(target.posZ - m.tz);
        if (dv > 1.0 || dt > 1.0) return null;
        return m.seen;
    }

    public static void trackerStore(EntityPlayerMP viewer, Entity target, boolean seen) {
        if (!memoOn(cfg().trackerSightMemoTicks)) return;
        Int2ObjectOpenHashMap<Memo> byTarget = TRACKER.computeIfAbsent(viewer, v -> new Int2ObjectOpenHashMap<>());
        Memo m = byTarget.get(target.getEntityId());
        if (m == null) {
            m = new Memo();
            byTarget.put(target.getEntityId(), m);
        }
        long now = viewer.world.getTotalWorldTime();
        m.tick = now;
        m.seen = seen;
        m.target = target.getUniqueID();
        m.dim = viewer.dimension;
        m.vx = viewer.posX;
        m.vy = viewer.posY;
        m.vz = viewer.posZ;
        m.tx = target.posX;
        m.ty = target.posY;
        m.tz = target.posZ;
        if (now >= nextPrune) {
            nextPrune = now + 200L;
            prune(now);
        }
    }

    private static void prune(long now) {
        for (Iterator<Int2ObjectOpenHashMap<Memo>> it = TRACKER.values().iterator(); it.hasNext(); ) {
            Int2ObjectOpenHashMap<Memo> byTarget = it.next();
            byTarget.values().removeIf(m -> now - m.tick > 40L);
            if (byTarget.isEmpty()) {
                it.remove();
            }
        }
    }

    // --- visibility packet memo ---

    public static LinkedHashMap<Entity, Double> seenLookup(EntityPlayerMP player) {
        int ticks = cfg().visibilityPacketMemoTicks;
        if (!memoOn(ticks)) return null;
        SeenMemo m = SEEN.get(player);
        if (m == null) return null;
        long age = player.world.getTotalWorldTime() - m.tick;
        if (age < 0 || age >= ticks || m.dim != player.dimension) return null;
        return m.map;
    }

    public static void seenStore(EntityPlayerMP player, LinkedHashMap<Entity, Double> map) {
        if (!memoOn(cfg().visibilityPacketMemoTicks)) return;
        SeenMemo m = SEEN.computeIfAbsent(player, p -> new SeenMemo());
        m.tick = player.world.getTotalWorldTime();
        m.dim = player.dimension;
        m.map = map;
    }

    private static double sq(double v) {
        return v * v;
    }
}
