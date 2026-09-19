package com.apocollis.aqtweaks.spawning;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import mcjty.tools.cache.StructureCache;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * InControl {@code StructureCache} lookups. Loaded only when InControl is present.
 */
public final class SpawnStructureExemption {

    private SpawnStructureExemption() {}

    public static Set<String> structuresAt(World world, BlockPos pos) {
        if (world == null || pos == null) {
            return Collections.emptySet();
        }
        Set<String> names = SpawnStructureLists.structureNames();
        if (names.isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> cached = SpawnStructureHitCache.get(world, pos);
        if (cached != null) {
            return cached;
        }
        Set<String> hit = new HashSet<>();
        for (String name : names) {
            if (StructureCache.CACHE.isInStructure(world, name, pos)) {
                hit.add(name);
            }
        }
        Set<String> frozen = hit.isEmpty() ? Set.of() : Set.copyOf(hit);
        SpawnStructureHitCache.put(world, pos, frozen);
        return frozen;
    }

    public static boolean keepSurfaceMob(String entityId, Set<String> structuresHere) {
        if (entityId == null || structuresHere == null || structuresHere.isEmpty()) {
            return false;
        }
        Set<String> listed = SpawnStructureLists.structuresFor(entityId);
        if (listed.isEmpty()) {
            return false;
        }
        for (String name : listed) {
            if (structuresHere.contains(name)) {
                return true;
            }
        }
        return false;
    }
}
