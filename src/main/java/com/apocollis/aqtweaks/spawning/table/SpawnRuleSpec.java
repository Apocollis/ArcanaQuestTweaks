package com.apocollis.aqtweaks.spawning.table;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * One potential-spawn row, as {@code regenerate_potentialspawn.py} writes it: either one mob for a dimension (and
 * optionally a biome list, a structure, a height cap and a water-block test) or a list of mobs to remove from a
 * dimension. Holds plain data only, so the table can be built and compared without a running game.
 */
public final class SpawnRuleSpec {
    public static final String MONSTER = "MONSTER";
    public static final String WATER_CREATURE = "WATER_CREATURE";

    public final int dimension;
    /** Sorted biome registry ids; null means any biome. */
    public final List<String> biomes;
    /** Structure name; null means anywhere. */
    public final String structure;
    /** The spawn position must be at or below this Y; null means no limit. */
    public final Integer maxHeight;
    /** The mob id; null for a removal row. */
    public final String mob;
    public final int weight;
    public final int groupMin;
    public final int groupMax;
    /** The per-player cap on this mob. */
    public final int limit;
    /** The row spawns water creatures and needs water under the spawn position. */
    public final boolean water;
    /** The mob ids a removal row takes out of the list; null for a spawn row. */
    public final List<String> remove;

    private final Set<String> biomeSet;

    private SpawnRuleSpec(int dimension, List<String> biomes, String structure, Integer maxHeight, String mob,
                          int weight, int groupMin, int groupMax, int limit, boolean water, List<String> remove) {
        this.dimension = dimension;
        this.biomes = biomes == null ? null : Collections.unmodifiableList(new ArrayList<String>(biomes));
        this.structure = structure;
        this.maxHeight = maxHeight;
        this.mob = mob;
        this.weight = weight;
        this.groupMin = groupMin;
        this.groupMax = groupMax;
        this.limit = limit;
        this.water = water;
        this.remove = remove == null ? null : Collections.unmodifiableList(new ArrayList<String>(remove));
        this.biomeSet = biomes == null ? null : new HashSet<String>(biomes);
    }

    public static SpawnRuleSpec spawn(int dimension, List<String> biomes, String structure, Integer maxHeight,
                                      String mob, int weight, int groupMin, int groupMax, int limit, boolean water) {
        return new SpawnRuleSpec(dimension, biomes, structure, maxHeight, mob, weight, groupMin, groupMax, limit,
                water, null);
    }

    public static SpawnRuleSpec removal(int dimension, List<String> mobs) {
        return new SpawnRuleSpec(dimension, null, null, null, null, 0, 0, 0, 0, false, mobs);
    }

    public boolean isRemoval() {
        return remove != null;
    }

    /** The creature type the row is limited to; null for a removal row, which applies to every type. */
    public String creatureType() {
        if (remove != null) {
            return null;
        }
        return water ? WATER_CREATURE : MONSTER;
    }

    /**
     * Whether the row can apply to a query of this dimension, biome and creature type. The structure, height, water
     * and count tests still have to pass at the spawn position.
     */
    public boolean appliesTo(int queryDimension, String biome, String creatureType) {
        if (dimension != queryDimension) {
            return false;
        }
        if (remove == null && !creatureType().equals(creatureType)) {
            return false;
        }
        return biomeSet == null || biomeSet.contains(biome);
    }

    /** The row as the JSON object In Control reads, keys in the order the script writes them. */
    public JsonObject toJson() {
        JsonObject rule = new JsonObject();
        rule.addProperty("dimension", dimension);
        if (remove != null) {
            JsonArray list = new JsonArray();
            for (String id : remove) {
                list.add(new com.google.gson.JsonPrimitive(id));
            }
            rule.add("remove", list);
            return rule;
        }
        if (biomes != null) {
            JsonArray list = new JsonArray();
            for (String id : biomes) {
                list.add(new com.google.gson.JsonPrimitive(id));
            }
            rule.add("biomereg", list);
        }
        if (structure != null) {
            rule.addProperty("structure", structure);
        }
        if (maxHeight != null) {
            rule.addProperty("maxheight", maxHeight);
        }
        JsonObject maxcount = new JsonObject();
        maxcount.addProperty("amount", limit);
        maxcount.addProperty("mob", mob);
        maxcount.addProperty("perplayer", true);
        rule.add("maxcount", maxcount);
        JsonArray mobs = new JsonArray();
        JsonObject entry = new JsonObject();
        entry.addProperty("mob", mob);
        entry.addProperty("weight", weight);
        entry.addProperty("groupcountmin", groupMin);
        entry.addProperty("groupcountmax", groupMax);
        mobs.add(entry);
        rule.add("mobs", mobs);
        rule.addProperty("creaturetype", creatureType());
        if (water) {
            JsonArray block = new JsonArray();
            block.add(new com.google.gson.JsonPrimitive("minecraft:water"));
            rule.add("block", block);
        }
        return rule;
    }

    @Override
    public String toString() {
        return toJson().toString();
    }
}
