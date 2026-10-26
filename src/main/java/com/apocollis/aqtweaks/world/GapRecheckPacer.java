package com.apocollis.aqtweaks.world;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.pacing.StewardPacing;
import com.apocollis.aqtweaks.pacing.TickSlice;
import com.apocollis.aqtweaks.perf.StageTimer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

/**
 * Paces vanilla's server-side {@code Chunk.recheckGaps}. {@code Chunk.onTick} calls it with {@code onlyOne = false}
 * on the server, so every flagged skylight column of a chunk is rechecked in one call; a big structure paste flags
 * columns in dozens of chunks and one tick then ran for 18 s (profile {@code 3heUXWJhoX}). The client already passes
 * {@code onlyOne = true} (one column per call). Here the server does the same, and a call is skipped when this
 * tick's shared slice ({@link TickSlice}, sized by {@link StewardPacing}) is spent. The gap flags stay set, so work
 * resumes on the next tick, exactly as vanilla does on the client. A floor of {@value #FLOOR} columns per tick keeps
 * it progressing. Chunks near a player draw from a larger slice.
 */
public final class GapRecheckPacer {

    private static final int FLOOR = 2;
    private static final int MAX_UNITS_NO_CLOCK = 24;
    private static final int MAX_UNITS_NO_CLOCK_NEAR = 96;

    private static int tickId = Integer.MIN_VALUE;
    private static int units;
    private static boolean clock;
    private static boolean fullTps;
    private static double normalMs;
    private static double nearMs;

    private GapRecheckPacer() {}

    private static ArcanaQuestTweaksConfig.DepthsModuleConfig.General cfg() {
        return ArcanaQuestTweaksConfig.DepthsModuleConfig.general;
    }

    public static boolean active(World world) {
        return world != null && !world.isRemote && cfg().paceChunkGapRecheck;
    }

    /** True when this recheck call must be skipped (retried next tick). Counts an allowed call as one unit. */
    public static boolean skip(World world, int chunkX, int chunkZ) {
        if (!active(world)) return false;
        MinecraftServer server = world.getMinecraftServer();
        int t = server == null ? tickId : server.getTickCounter();
        if (server == null || t != tickId) {
            tickId = t;
            units = 0;
            ArcanaQuestTweaksConfig.DepthsModuleConfig.General c = cfg();
            double n = StewardPacing.sliceBudgetMs(c.gapRecheckMaxSliceMs, 0.5);
            double f = StewardPacing.sliceBudgetMs(Math.max(c.gapRecheckMaxSliceMs, c.gapRecheckNearSliceMs), 0.5);
            clock = n >= 0.0;
            normalMs = n;
            nearMs = Math.max(n, f);
            fullTps = clock && StewardPacing.getLoadFactor() >= 1.0;
        }
        boolean near = playerWithin(world, chunkX, chunkZ, cfg().gapRecheckNearChunks);
        boolean allow;
        if (units < FLOOR) {
            allow = true;
        } else if (!clock) {
            allow = units < (near ? MAX_UNITS_NO_CLOCK_NEAR : MAX_UNITS_NO_CLOCK);
        } else {
            double limit = near ? nearMs : normalMs;
            allow = limit > 0.0 && TickSlice.elapsedMs(server) < limit
                    && (fullTps || StewardPacing.remainingMs(40.0) >= 2.0);
        }
        if (allow) {
            units++;
            StageTimer.recordNs("light.gap.cols", 0L, near ? "near" : null);
            return false;
        }
        StageTimer.recordNs("light.gap.skipped", 0L, null);
        return true;
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
}
