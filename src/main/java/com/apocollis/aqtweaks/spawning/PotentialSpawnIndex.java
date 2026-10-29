package com.apocollis.aqtweaks.spawning;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import mcjty.incontrol.ForgeEventHandlers;
import mcjty.incontrol.rules.PotentialSpawnRule;
import mcjty.incontrol.rules.RulesManager;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.event.world.WorldEvent;

/**
 * In Control runs every potential-spawn rule for every spawn-list query, and each rule that names biomes asks the
 * world for the biome again. With 150 rules that was about 7 percent of the server thread. The rules' dimension, biome
 * and creature type do not change, so the rules that can match a (dimension, biome, type) are worked out once and only
 * those run through In Control's own matching, in the original order. Rules that restrict something else (blocks,
 * structures, heights, counts, time) keep all of their checks; this only removes rules that can never match.
 */
public final class PotentialSpawnIndex {
    private static final class Key {
        final int dimension;
        final String biome;
        final EnumCreatureType type;
        final int hash;

        Key(int dimension, String biome, EnumCreatureType type) {
            this.dimension = dimension;
            this.biome = biome;
            this.type = type;
            this.hash = (dimension * 31 + biome.hashCode()) * 31 + (type == null ? 0 : type.ordinal());
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Key)) {
                return false;
            }
            Key key = (Key) other;
            return key.dimension == dimension && key.type == type && key.biome.equals(biome);
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }

    private static final class Table {
        final int size;
        final Object first;
        final Object last;
        final Map<Key, PotentialSpawnRule[]> candidates = new ConcurrentHashMap<Key, PotentialSpawnRule[]>();

        Table(int size, Object first, Object last) {
            this.size = size;
            this.first = first;
            this.last = last;
        }
    }

    private static volatile Table table = new Table(-1, null, null);

    private PotentialSpawnIndex() {
    }

    /** Reads the restricting fields from a rule's JSON; called when In Control parses the rule. */
    public static void capture(JsonElement element, PotentialSpawnRule rule) {
        if (!(rule instanceof IndexedSpawnRule) || element == null || !element.isJsonObject()) {
            return;
        }
        try {
            JsonObject json = element.getAsJsonObject();
            String type = null;
            if (json.has("creaturetype") && json.get("creaturetype").isJsonPrimitive()) {
                try {
                    type = EnumCreatureType.valueOf(json.get("creaturetype").getAsString()).name();
                } catch (IllegalArgumentException invalid) {
                    type = null; // In Control logs it and treats the rule as any type
                }
            }
            int[] dimensions = null;
            if (json.has("dimension")) {
                List<Integer> list = new ArrayList<Integer>();
                for (JsonElement value : asList(json.get("dimension"))) {
                    list.add(value.getAsInt());
                }
                dimensions = new int[list.size()];
                for (int i = 0; i < dimensions.length; i++) {
                    dimensions[i] = list.get(i);
                }
            }
            Set<String> biomes = null;
            if (json.has("biomereg")) {
                biomes = new HashSet<String>();
                for (JsonElement value : asList(json.get("biomereg"))) {
                    biomes.add(new ResourceLocation(value.getAsString()).toString());
                }
            }
            ((IndexedSpawnRule) rule).aqtweaks$setIndex(type, dimensions, biomes);
        } catch (RuntimeException unreadable) {
            ((IndexedSpawnRule) rule).aqtweaks$setIndex(null, null, null);
        }
    }

    private static List<JsonElement> asList(JsonElement element) {
        List<JsonElement> out = new ArrayList<JsonElement>();
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            for (JsonElement value : array) {
                out.add(value);
            }
        } else {
            out.add(element);
        }
        return out;
    }

    /** Whether a rule with these restrictions can match a query of this dimension, biome and type. */
    static boolean couldMatch(String ruleType, int[] ruleDimensions, Set<String> ruleBiomes,
                              EnumCreatureType queryType, int dimension, String biome) {
        if (ruleType != null && (queryType == null || !ruleType.equals(queryType.name()))) {
            return false;
        }
        if (ruleDimensions != null) {
            boolean found = false;
            for (int id : ruleDimensions) {
                if (id == dimension) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return ruleBiomes == null || ruleBiomes.contains(biome);
    }

    /** True when the event was handled here; false leaves it to In Control's own loop. */
    public static boolean handle(WorldEvent.PotentialSpawns event) {
        com.apocollis.aqtweaks.ArcanaQuestTweaksConfig.SpawningGeneral general =
                com.apocollis.aqtweaks.ArcanaQuestTweaksConfig.SpawningModuleConfig.general;
        if (!general.enable || ForgeEventHandlers.debug) {
            return false;
        }
        boolean tabled = PotentialSpawnTable.apply(event);
        if (tabled && !general.potentialSpawnTableKeepsInControlRules) {
            return true;
        }
        if (!general.indexPotentialSpawnRules) {
            return false; // In Control's own loop runs, after the table when that added its entries
        }
        List<PotentialSpawnRule> all = RulesManager.potentialSpawnRules;
        World world = event.getWorld();
        if (world == null) {
            return false;
        }
        PotentialSpawnRule[] rules;
        try {
            int size = all.size();
            if (size == 0) {
                return true;
            }
            Table current = table;
            Object first = all.get(0);
            Object last = all.get(size - 1);
            if (current.size != size || current.first != first || current.last != last) {
                current = new Table(size, first, last);
                table = current;
            }
            Biome biome = world.getBiome(event.getPos());
            ResourceLocation name = biome.getRegistryName();
            String biomeName = name == null ? "" : name.toString();
            int dimension = world.provider.getDimension();
            Key key = new Key(dimension, biomeName, event.getType());
            rules = current.candidates.get(key);
            if (rules == null) {
                List<PotentialSpawnRule> kept = new ArrayList<PotentialSpawnRule>();
                for (PotentialSpawnRule rule : new ArrayList<PotentialSpawnRule>(all)) {
                    IndexedSpawnRule indexed = (IndexedSpawnRule) rule;
                    if (couldMatch(indexed.aqtweaks$creatureType(), indexed.aqtweaks$dimensions(),
                            indexed.aqtweaks$biomes(), event.getType(), dimension, biomeName)) {
                        kept.add(rule);
                    }
                }
                rules = kept.toArray(new PotentialSpawnRule[0]);
                current.candidates.put(key, rules);
            }
        } catch (RuntimeException unexpected) {
            return false; // a reload in progress, or a rule this could not index: In Control's own loop decides
        }
        for (PotentialSpawnRule rule : rules) {
            if (!rule.match(event)) {
                continue;
            }
            if (rule.shouldRemoveAll()) {
                event.getList().clear();
            } else {
                for (Class<?> clazz : rule.getToRemoveMobs()) {
                    for (int index = event.getList().size() - 1; index >= 0; index--) {
                        if (event.getList().get(index).entityClass == clazz) {
                            event.getList().remove(index);
                        }
                    }
                }
            }
            for (Biome.SpawnListEntry entry : rule.getSpawnEntries()) {
                event.getList().add(entry);
            }
        }
        return true;
    }
}
