package com.apocollis.aqtweaks.spawning.table;

import com.google.gson.JsonArray;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Builds the rows {@code regenerate_potentialspawn.py} writes to {@code potentialspawn.json}, in the same order and
 * with the same values, from the classification files alone. The Overworld rows come first (one per mob, sorted by
 * id), then the Nether, End, Beneath and dimension-wide Overworld mobs, the structure rows, the depths pool, and the
 * row that takes the vanilla mobs out of the Beneath.
 */
public final class SpawnTableBuilder {
    private static final List<String> DARK_EXCEPTIONS = Arrays.asList(
            "lycanitesmobs:beholder", "lycanitesmobs:shade", "lycanitesmobs:darkling");
    private static final List<String> BENEATH_VANILLA = Arrays.asList(
            "minecraft:zombie", "minecraft:skeleton", "minecraft:creeper", "minecraft:spider",
            "minecraft:cave_spider", "minecraft:slime", "minecraft:witch", "minecraft:enderman",
            "minecraft:zombie_villager", "minecraft:husk", "minecraft:stray", "minecraft:vindication_illager",
            "minecraft:illusion_illager", "minecraft:evocation_illager");
    private static final int DEFAULT_DEPTHS_HEIGHT = -30;
    private static final int UNDERGROUND_WEIGHT_BONUS = 10;

    private final SpawnTableSources sources;
    private final Map<String, String> mobToTier = new HashMap<String, String>();
    private final Set<String> undergroundOnly;

    private SpawnTableBuilder(SpawnTableSources sources) {
        this.sources = sources;
        for (Map.Entry<String, List<String>> tier : sources.tiers.entrySet()) {
            for (String mob : tier.getValue()) {
                mobToTier.put(mob, tier.getKey());
            }
        }
        Set<String> only = new HashSet<String>(sources.underground);
        only.removeAll(sources.surface);
        only.removeAll(sources.disabled);
        only.removeAll(sources.waterMobs);
        undergroundOnly = only;
    }

    public static List<SpawnRuleSpec> build(SpawnTableSources sources) {
        return new SpawnTableBuilder(sources).build();
    }

    /** The rows as the JSON array the script would write. */
    public static JsonArray toJson(List<SpawnRuleSpec> rules) {
        JsonArray array = new JsonArray();
        for (SpawnRuleSpec rule : rules) {
            array.add(rule.toJson());
        }
        return array;
    }

    private List<SpawnRuleSpec> build() {
        List<SpawnRuleSpec> rules = new ArrayList<SpawnRuleSpec>();

        Set<String> extraMobs = new HashSet<String>();
        for (List<String> mobs : sources.extraSpawns.values()) {
            extraMobs.addAll(mobs);
        }
        Set<String> netherAll = new HashSet<String>(listOf(sources.biomeTypes, "nether"));
        Set<String> endAll = new HashSet<String>(listOf(sources.biomeTypes, "end"));
        Set<String> darkToFilter = new HashSet<String>(listOf(sources.biomeTypes, "dark"));
        darkToFilter.removeAll(DARK_EXCEPTIONS);

        Map<String, Set<String>> mobBiomes = new TreeMap<String, Set<String>>();
        for (Map.Entry<String, List<String>> category : sources.biomeTypes.entrySet()) {
            String name = category.getKey();
            if (name.equals("nether") || name.equals("end") || name.equals("dark") || name.equals("any")) {
                continue;
            }
            List<String> biomes = listOf(sources.biomeMappings, name);
            List<String> active = new ArrayList<String>();
            for (String mob : category.getValue()) {
                if (!sources.disabled.contains(mob) && !extraMobs.contains(mob)) {
                    active.add(mob);
                }
            }
            if (biomes.isEmpty() || active.isEmpty() || name.equals("beneath")) {
                continue;
            }
            for (String mob : active) {
                if (netherAll.contains(mob) || endAll.contains(mob) || darkToFilter.contains(mob)) {
                    continue;
                }
                Set<String> set = mobBiomes.get(mob);
                if (set == null) {
                    set = new TreeSet<String>();
                    mobBiomes.put(mob, set);
                }
                set.addAll(biomes);
            }
        }
        for (Map.Entry<String, Set<String>> entry : mobBiomes.entrySet()) {
            rules.add(spawnRow(0, new ArrayList<String>(entry.getValue()), null, null, entry.getKey()));
        }

        addDimension(rules, -1, active(listOf(sources.biomeTypes, "nether")));
        addDimension(rules, 1, active(listOf(sources.biomeTypes, "end")));
        addDimension(rules, 10, active(listOf(sources.biomeTypes, "dark")));
        addDimension(rules, 0, active(listOf(sources.biomeTypes, "any")));

        for (String structure : new TreeSet<String>(sources.structures.keySet())) {
            for (String mob : active(sources.structures.get(structure))) {
                rules.add(spawnRow(0, null, structure, null, mob));
            }
        }

        for (SpawnTableSources.DepthsEntry depths : sources.depths) {
            SpawnTableSources.TierParams tier = tierOf(depths.mob);
            rules.add(SpawnRuleSpec.spawn(0, null, null,
                    depths.maxHeight != null ? depths.maxHeight : DEFAULT_DEPTHS_HEIGHT,
                    depths.mob,
                    depths.weight != null ? depths.weight : tier.weight,
                    depths.groupMin != null ? depths.groupMin : tier.groupMin,
                    depths.groupMax != null ? depths.groupMax : tier.groupMax,
                    depths.maxCount != null ? depths.maxCount : tier.limit,
                    sources.waterMobs.contains(depths.mob)));
        }

        rules.add(SpawnRuleSpec.removal(10, BENEATH_VANILLA));
        return Collections.unmodifiableList(rules);
    }

    private void addDimension(List<SpawnRuleSpec> rules, int dimension, List<String> mobs) {
        for (String mob : mobs) {
            rules.add(spawnRow(dimension, null, null, null, mob));
        }
    }

    /** The listed mobs that are not disabled, sorted; a mob listed twice stays twice, as in the script. */
    private List<String> active(List<String> mobs) {
        List<String> out = new ArrayList<String>();
        if (mobs != null) {
            for (String mob : mobs) {
                if (!sources.disabled.contains(mob)) {
                    out.add(mob);
                }
            }
        }
        Collections.sort(out);
        return out;
    }

    private SpawnRuleSpec spawnRow(int dimension, List<String> biomes, String structure, Integer maxHeight,
                                   String mob) {
        SpawnTableSources.TierParams tier = tierOf(mob);
        int weight = tier.weight + (undergroundOnly.contains(mob) ? UNDERGROUND_WEIGHT_BONUS : 0);
        return SpawnRuleSpec.spawn(dimension, biomes, structure, maxHeight, mob, weight, tier.groupMin,
                tier.groupMax, tier.limit, sources.waterMobs.contains(mob));
    }

    private SpawnTableSources.TierParams tierOf(String mob) {
        String tier = mobToTier.get(mob);
        SpawnTableSources.TierParams params = tier == null ? null : sources.tierParams.get(tier);
        return params != null ? params : sources.tierParams.get("common");
    }

    private static List<String> listOf(Map<String, List<String>> map, String key) {
        List<String> list = map.get(key);
        return list == null ? Collections.<String>emptyList() : list;
    }
}
