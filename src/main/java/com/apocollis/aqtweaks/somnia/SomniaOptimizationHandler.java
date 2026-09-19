package com.apocollis.aqtweaks.somnia;

import com.kingrunes.somnia.Somnia;
import com.kingrunes.somnia.common.SomniaConfig;
import com.kingrunes.somnia.common.util.SomniaState;
import com.kingrunes.somnia.server.ServerTickHandler;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

public class SomniaOptimizationHandler {

    private static final Long2LongOpenHashMap LAST_CHECK_TICKS = new Long2LongOpenHashMap();
    private static long lastPruneTick = -1L;

    static {
        LAST_CHECK_TICKS.defaultReturnValue(-1L);
    }

    private static long chunkKey(int dim, int x, int z) {
        return (((long) dim) << 48) ^ (((long) x & 0xFFFFFFL) << 24) ^ ((long) z & 0xFFFFFFL);
    }

    public static synchronized void chunkLightCheck(Chunk chunk) {
        if (chunk == null) return;
        World world = chunk.getWorld();
        if (world == null || world.isRemote) return;

        long currentTick = world.getTotalWorldTime();
        int dim = world.provider != null ? world.provider.getDimension() : 0;
        long key = chunkKey(dim, chunk.x, chunk.z);

        // 1. Never invoke checkLight more than once per tick for any chunk
        if (LAST_CHECK_TICKS.get(key) == currentTick) {
            return;
        }

        // Periodic maintenance to prevent unbounded memory growth over long server uptimes
        if (lastPruneTick < 0 || currentTick - lastPruneTick > 1200L || currentTick < lastPruneTick) {
            lastPruneTick = currentTick;
            if (LAST_CHECK_TICKS.size() > 1024) {
                LAST_CHECK_TICKS.long2LongEntrySet().removeIf(entry -> Math.abs(currentTick - entry.getLongValue()) > 100L);
            }
        }

        // 2. Check Somnia sleep state
        boolean isAsleep = false;
        if (Somnia.instance != null && Somnia.instance.tickHandlers != null) {
            for (ServerTickHandler handler : Somnia.instance.tickHandlers) {
                if (handler != null && handler.worldServer == world) {
                    if (handler.currentState == SomniaState.ACTIVE) {
                        isAsleep = true;
                    }
                    break;
                }
            }
        }

        if (isAsleep) {
            // Respect Somnia config: skip checkLight during active sleep if configured
            if (SomniaConfig.PERFORMANCE != null && SomniaConfig.PERFORMANCE.disableMoodSoundAndLightCheck) {
                return;
            }
            LAST_CHECK_TICKS.put(key, currentTick);
            chunk.checkLight();
            return;
        }

        // 3. Player is awake: throttle checkLight even when queuedLightChecks < 4096.
        // Depths tall columns rarely return to vanilla idle 4096, so a backlog flush every
        // tick never drains; the 20-tick round-robin still processes the queue over time.
        if (Math.floorMod(currentTick + chunk.x + chunk.z, 20L) != 0) {
            return;
        }
        LAST_CHECK_TICKS.put(key, currentTick);
        chunk.checkLight();
    }
}
