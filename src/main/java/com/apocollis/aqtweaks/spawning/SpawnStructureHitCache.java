package com.apocollis.aqtweaks.spawning;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Bounded per-chunk InControl structure-hit sets. No InControl import — safe from always-on classes.
 */
public final class SpawnStructureHitCache {

    private static final int CAP = 4096;
    private static final Map<CacheKey, Set<String>> HITS =
            Collections.synchronizedMap(new LinkedHashMap<>(256, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<CacheKey, Set<String>> eldest) {
                    return size() > CAP;
                }
            });

    private record CacheKey(int worldId, int cx, int cz) {}

    private SpawnStructureHitCache() {}

    public static Set<String> get(World world, BlockPos pos) {
        if (world == null || pos == null) {
            return null;
        }
        return HITS.get(key(world, pos));
    }

    public static void put(World world, BlockPos pos, Set<String> hit) {
        if (world == null || pos == null || hit == null) {
            return;
        }
        HITS.put(key(world, pos), hit);
    }

    public static void clear() {
        HITS.clear();
    }

    public static void clearWorld(World world) {
        if (world == null) {
            return;
        }
        int id = System.identityHashCode(world);
        synchronized (HITS) {
            HITS.keySet().removeIf(k -> k.worldId() == id);
        }
    }

    private static CacheKey key(World world, BlockPos pos) {
        return new CacheKey(System.identityHashCode(world), pos.getX() >> 4, pos.getZ() >> 4);
    }
}
