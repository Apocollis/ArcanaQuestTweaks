package com.apocollis.aqtweaks.spawning;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.gen.structure.MapGenStructureData;

/**
 * Expand InControl {@code StructureCache} origin chunks with every feature/child {@code BB}.
 * Memoizes the union per {@link MapGenStructureData} until feature/child counts change.
 */
public final class StructureCacheHooks {

    private static final Map<MapGenStructureData, Cached> EXPANDED = new WeakHashMap<>();

    private StructureCacheHooks() {}

    /**
     * Union of stock origin chunks plus every feature/child {@code BB}. Cached until the
     * structure NBT fingerprint changes. Caller should return this set from parse.
     */
    public static Set<Long> expandedChunks(MapGenStructureData data, Set<Long> stock) {
        if (data == null) {
            return stock;
        }
        NBTTagCompound features = data.getTagCompound();
        if (features == null) {
            return stock;
        }
        int fingerprint = fingerprint(features);
        synchronized (EXPANDED) {
            Cached hit = EXPANDED.get(data);
            if (hit != null && hit.fingerprint == fingerprint) {
                return hit.chunks;
            }
        }
        LongOpenHashSet expanded = new LongOpenHashSet();
        if (stock != null && !stock.isEmpty()) {
            expanded.addAll(stock);
        }
        addBoundingBoxChunks(features, expanded);
        synchronized (EXPANDED) {
            EXPANDED.put(data, new Cached(fingerprint, expanded));
        }
        return expanded;
    }

    private static void addBoundingBoxChunks(NBTTagCompound features, Set<Long> chunks) {
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

    private static int fingerprint(NBTTagCompound features) {
        int fp = features.getKeySet().size();
        for (String key : features.getKeySet()) {
            NBTBase raw = features.getTag(key);
            if (!(raw instanceof NBTTagCompound compound)) {
                continue;
            }
            fp = 31 * fp + compound.getTagList("Children", 10).tagCount();
        }
        return fp;
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

    private record Cached(int fingerprint, LongOpenHashSet chunks) {}
}
