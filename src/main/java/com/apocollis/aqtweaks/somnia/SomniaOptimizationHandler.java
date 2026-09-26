package com.apocollis.aqtweaks.somnia;

import com.apocollis.aqtweaks.mixin.vanilla.AccessorChunk;
import com.kingrunes.somnia.Somnia;
import com.kingrunes.somnia.common.SomniaConfig;
import com.kingrunes.somnia.common.util.SomniaState;
import com.kingrunes.somnia.server.ServerTickHandler;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import net.minecraft.entity.player.EntityPlayer;
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

    private static boolean isNearAwakePlayer(World world, int chunkX, int chunkZ, int chunkRadius) {
        double maxDistSq = (chunkRadius * 16.0 + 8.0) * (chunkRadius * 16.0 + 8.0);
        double chunkMidX = (chunkX << 4) + 8.0;
        double chunkMidZ = (chunkZ << 4) + 8.0;
        for (EntityPlayer player : world.playerEntities) {
            if (player.isSpectator()) continue;
            double dx = player.posX - chunkMidX;
            double dz = player.posZ - chunkMidZ;
            if (dx * dx + dz * dz <= maxDistSq) {
                return true;
            }
        }
        return false;
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
        // Direct accessor check for queued light checks (4096 = idle).
        // If the chunk has a backlog (< 4096) AND is within player proximity (2 chunks / ~32 blocks),
        // drain priority every tick so village houses and immediate surroundings relight within seconds.
        // Distant background chunks (> 2 chunks away) and already idle chunks stay throttled to once
        // every 20 ticks to eliminate exploration / flight hitching.
        int queued = ((AccessorChunk) chunk).getQueuedLightChecks();
        boolean needsPriority = queued < 4096 && isNearAwakePlayer(world, chunk.x, chunk.z, 2);

        if (!needsPriority) {
            if (Math.floorMod(currentTick + chunk.x + chunk.z, 20L) != 0) {
                return;
            }
        }

        LAST_CHECK_TICKS.put(key, currentTick);
        chunk.checkLight();
    }
}
