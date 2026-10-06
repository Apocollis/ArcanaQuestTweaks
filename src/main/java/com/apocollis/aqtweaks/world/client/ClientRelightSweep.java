package com.apocollis.aqtweaks.world.client;

import com.apocollis.aqtweaks.ArcanaQuestTweaks;
import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import it.unimi.dsi.fastutil.longs.Long2LongLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.LogManager;

import java.util.ArrayDeque;

/**
 * Client copy of the chunk light, repaired when a chunk arrives.
 *
 * <p>Light reaches the client only inside chunk data (or when the client runs its own check for a block
 * change it receives). Emitters that the server lit after it had already sent the chunk, or whose client
 * check was skipped because the surrounding chunks were not there yet, stay dark on the client until
 * something forces a check; placing a torch nearby does exactly that. Vanilla's own sweep for dark
 * emitters ({@code Chunk.enqueueRelightChecks}) only runs on the server world.
 *
 * <p>For each chunk the client loads: once its neighbors exist (or after a wait), scan its non-empty
 * sections for light emitters whose stored block light, or that of an open neighbor cell, is lower than
 * the emitter should produce, and run {@code World.checkLight} for those. Work is spread over client
 * ticks (a couple of sections and a few checks per tick), runs on the client thread only, and never
 * touches the server world.
 */
@Mod.EventBusSubscriber(value = Side.CLIENT, modid = ArcanaQuestTweaks.MODID)
public final class ClientRelightSweep {

    private static final int SECTIONS_PER_TICK = 2;
    private static final int CHECKS_PER_TICK = 16;
    private static final int MAX_JOBS = 512;
    private static final int MAX_PENDING = 8192;
    private static final int MAX_STALE_PER_SECTION = 64;
    private static final int EXAMINE_PER_TICK = 96;
    private static final double RANGE_SQ = 128.0 * 128.0;
    private static final int MAX_TRIED = 65536;
    private static final long MAX_WAIT_TICKS = 600L;
    private static final long MAX_PENDING_AGE = 2400L;

    private static final ArrayDeque<ChunkJob> JOBS = new ArrayDeque<>();
    /** Emitter position (packed) to the client tick it was found stale. Insertion order = retry order. */
    private static final Long2LongLinkedOpenHashMap PENDING = new Long2LongLinkedOpenHashMap();
    /** Positions already re-checked this session; a check that changes nothing must not be queued again. */
    private static final LongOpenHashSet TRIED = new LongOpenHashSet();
    private static World lastWorld;
    private static long tick;
    private static long nextLog;
    private static int statSections;
    private static int statStale;
    private static int statChecks;

    private ClientRelightSweep() {}

    private static final class ChunkJob {
        final int x;
        final int z;
        final long created;
        int nextSection;

        ChunkJob(int x, int z, long created) {
            this.x = x;
            this.z = z;
            this.created = created;
        }
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getWorld() == null || !event.getWorld().isRemote
                || !ArcanaQuestTweaksConfig.ClientModuleConfig.relight.enable) {
            return;
        }
        Chunk chunk = event.getChunk();
        JOBS.addLast(new ChunkJob(chunk.x, chunk.z, tick));
        while (JOBS.size() > MAX_JOBS) {
            JOBS.pollFirst();
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        World world = mc.world;
        if (world != lastWorld) {
            JOBS.clear();
            PENDING.clear();
            TRIED.clear();
            lastWorld = world;
        }
        tick++;
        if (world == null || !ArcanaQuestTweaksConfig.ClientModuleConfig.relight.enable) {
            return;
        }
        scan(world);
        drain(world);
        if (ArcanaQuestTweaksConfig.ClientModuleConfig.relight.debug) {
            logStats();
        }
    }

    private static boolean neighborsLoaded(World world, int cx, int cz) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (world.getChunkProvider().getLoadedChunk(cx + dx, cz + dz) == null) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void scan(World world) {
        int sections = SECTIONS_PER_TICK;
        int examined = JOBS.size();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos near = new BlockPos.MutableBlockPos();
        while (sections > 0 && examined-- > 0 && !JOBS.isEmpty()) {
            ChunkJob job = JOBS.pollFirst();
            Chunk chunk = world.getChunkProvider().getLoadedChunk(job.x, job.z);
            if (chunk == null) {
                continue;
            }
            if (!neighborsLoaded(world, job.x, job.z) && tick - job.created < MAX_WAIT_TICKS) {
                JOBS.addLast(job);
                continue;
            }
            ExtendedBlockStorage[] storages = chunk.getBlockStorageArray();
            while (sections > 0 && job.nextSection < storages.length) {
                ExtendedBlockStorage storage = storages[job.nextSection++];
                sections--;
                if (storage == null || storage == Chunk.NULL_BLOCK_STORAGE || storage.isEmpty()) {
                    continue;
                }
                statSections++;
                scanSection(world, storage, job.x << 4, job.z << 4, pos, near);
            }
            if (job.nextSection < storages.length) {
                JOBS.addFirst(job);
            }
        }
    }

    private static void scanSection(World world, ExtendedBlockStorage storage, int baseX, int baseZ,
            BlockPos.MutableBlockPos pos, BlockPos.MutableBlockPos near) {
        int baseY = storage.getYLocation();
        int found = 0;
        for (int y = 0; y < 16; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    IBlockState state = storage.get(x, y, z);
                    int emit = state.getLightValue();
                    // Lava and other liquid emitters are by far the most numerous (underground lakes) and
                    // are not what players see go dark; they filled the queue and starved real torches.
                    if (emit <= 0 || state.getMaterial().isLiquid()) {
                        continue;
                    }
                    pos.setPos(baseX + x, baseY + y, baseZ + z);
                    long key = pos.toLong();
                    if (TRIED.contains(key) || PENDING.containsKey(key)) {
                        continue;
                    }
                    if (isStale(world, pos, near, emit)) {
                        PENDING.put(key, tick);
                        while (PENDING.size() > MAX_PENDING) {
                            PENDING.removeFirstLong();
                        }
                        statStale++;
                        if (++found >= MAX_STALE_PER_SECTION) {
                            return;
                        }
                    }
                }
            }
        }
    }

    /** True if the cell, or an open neighbor of it, holds less block light than this emitter should give. */
    private static boolean isStale(World world, BlockPos pos, BlockPos.MutableBlockPos near, int emit) {
        if (world.getLightFor(EnumSkyBlock.BLOCK, pos) < emit) {
            return true;
        }
        for (EnumFacing facing : EnumFacing.values()) {
            near.setPos(pos.getX() + facing.getXOffset(), pos.getY() + facing.getYOffset(),
                    pos.getZ() + facing.getZOffset());
            if (!world.isBlockLoaded(near)) {
                continue;
            }
            IBlockState state = world.getBlockState(near);
            if (state.getLightOpacity(world, near) >= 15) {
                continue;
            }
            if (world.getLightFor(EnumSkyBlock.BLOCK, near) < emit - 1) {
                return true;
            }
        }
        return false;
    }

    /**
     * Run checks for pending emitters near the player first: entries farther than 128 blocks, or whose
     * 17-block area is not loaded yet, go back to the end of the queue and are retried later.
     */
    private static void drain(World world) {
        net.minecraft.entity.player.EntityPlayer player = Minecraft.getMinecraft().player;
        int examine = Math.min(EXAMINE_PER_TICK, PENDING.size());
        int checks = 0;
        for (int i = 0; i < examine && checks < CHECKS_PER_TICK; i++) {
            long key = PENDING.firstLongKey();
            long found = PENDING.removeFirstLong();
            if (tick - found > MAX_PENDING_AGE) {
                continue;
            }
            BlockPos pos = BlockPos.fromLong(key);
            boolean near = player == null
                    || player.getDistanceSq(pos.getX() + 0.5, player.posY, pos.getZ() + 0.5) <= RANGE_SQ;
            if (near && world.isAreaLoaded(pos, 17, false)) {
                world.checkLight(pos);
                if (TRIED.size() > MAX_TRIED) {
                    TRIED.clear();
                }
                TRIED.add(key);
                checks++;
                statChecks++;
            } else {
                PENDING.put(key, found);
            }
        }
    }

    private static void logStats() {
        if (nextLog == 0L) {
            nextLog = tick + 1200L;
            return;
        }
        if (tick < nextLog) {
            return;
        }
        nextLog = tick + 1200L;
        if (statSections + statStale + statChecks > 0) {
            LogManager.getLogger("AQTweaks-Relight").info(
                    "[AQ-CLIENT-RELIGHT] last 60s: sections scanned={} stale emitters found={} checks run={} pending={} jobs={}",
                    statSections, statStale, statChecks, PENDING.size(), JOBS.size());
        }
        statSections = 0;
        statStale = 0;
        statChecks = 0;
    }
}
