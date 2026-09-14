package com.apocollis.aqtweaks.spawning;

import java.util.Set;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.gen.structure.MapGenStructureData;

/**
 * Expand InControl {@code StructureCache} origin chunks with every feature/child {@code BB}.
 */
public final class StructureCacheHooks {

    private StructureCacheHooks() {}

    public static void addBoundingBoxChunks(MapGenStructureData data, Set<Long> chunks) {
        if (data == null || chunks == null) {
            return;
        }
        NBTTagCompound features = data.getTagCompound();
        if (features == null) {
            return;
        }
        for (String key : features.getKeySet()) {
            NBTBase raw = features.getTag(key);
            if (!(raw instanceof NBTTagCompound compound)) {
                continue;
            }
            addBb(chunks, compound.getIntArray("BB"));
            NBTTagList children = compound.getTagList("Children", 10);
            for (int i = 0; i < children.tagCount(); i++) {
                addBb(chunks, children.getCompoundTagAt(i).getIntArray("BB"));
            }
        }
    }

    private static void addBb(Set<Long> chunks, int[] bb) {
        if (bb == null || bb.length < 6) {
            return;
        }
        int minCx = bb[0] >> 4;
        int maxCx = bb[3] >> 4;
        int minCz = bb[2] >> 4;
        int maxCz = bb[5] >> 4;
        if (minCx > maxCx) {
            int swap = minCx;
            minCx = maxCx;
            maxCx = swap;
        }
        if (minCz > maxCz) {
            int swap = minCz;
            minCz = maxCz;
            maxCz = swap;
        }
        for (int cx = minCx; cx <= maxCx; cx++) {
            for (int cz = minCz; cz <= maxCz; cz++) {
                chunks.add(ChunkPos.asLong(cx, cz));
            }
        }
    }
}
