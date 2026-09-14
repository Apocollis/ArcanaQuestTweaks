package com.apocollis.aqtweaks.somnia;

import com.apocollis.aqtweaks.util.Reflect;
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

        // 3. Player is awake:
        // Flush pending queued light checks immediately (queuedLightChecks < 4096 means checks are pending)
        int queued = Reflect.getQueuedLightChecks(chunk);
        boolean hasPendingBacklog = queued < 4096;

        // Pure ambient/mood check: throttle to once every 20 ticks (1 second) round-robin
        boolean ambientInterval = Math.floorMod(currentTick + chunk.x + chunk.z, 20L) == 0;

        if (hasPendingBacklog || ambientInterval) {
            LAST_CHECK_TICKS.put(key, currentTick);
            chunk.checkLight();
        }
    }
}
