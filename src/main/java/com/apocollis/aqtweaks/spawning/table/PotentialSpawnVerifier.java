package com.apocollis.aqtweaks.spawning.table;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Compares the table with an In Control {@code potentialspawn.json}. For every dimension, biome and creature type it
 * lists what each side would add to (or take from) the spawn list, together with the tests that still have to pass at
 * the spawn position, and reports every query where the two lists differ. The table side goes through
 * {@link SpawnRuleSpec#appliesTo}, the code the game uses, so a mistake in the lookup shows up here too.
 */
public final class PotentialSpawnVerifier {
    private static final int OTHER_DIMENSION = Integer.MIN_VALUE;
    private static final String OTHER_BIOME = "(no biome)";
    private static final int MAX_REPORTED = 25;
    private static final Set<String> HANDLED = new HashSet<String>(Arrays.asList(
            "dimension", "biomereg", "creaturetype", "structure", "maxheight", "block", "maxcount", "mobs",
            "remove"));

    /** The result of a comparison. */
    public static final class Report {
        public final int queriesChecked;
        public final List<String> differences;
        /** Rule fields the table does not implement, with how many rules use them. */
        public final List<String> unsupportedFields;
        /** The number of rules in the file, so a stale file can be told from a stale table. */
        public final int fileRules;
        public final int tableRules;

        Report(int queriesChecked, List<String> differences, List<String> unsupportedFields, int fileRules,
               int tableRules) {
            this.queriesChecked = queriesChecked;
            this.differences = differences;
            this.unsupportedFields = unsupportedFields;
            this.fileRules = fileRules;
            this.tableRules = tableRules;
        }

        public boolean identical() {
            return differences.isEmpty();
        }
    }

    private static final class FileRule {
        Set<Integer> dimensions;
        Set<String> biomes;
        String creatureType;
        final List<String> removes = new ArrayList<String>();
        final List<String> adds = new ArrayList<String>();
        String conditions = "";

        boolean appliesTo(int dimension, String biome, String type) {
            if (creatureType != null && !creatureType.equals(type)) {
                return false;
            }
            if (dimensions != null && !dimensions.contains(dimension)) {
                return false;
            }
            return biomes == null || biomes.contains(biome);
        }
    }

    private PotentialSpawnVerifier() {
    }

    /**
     * @param biomes       every biome registry id of the game; the ids the rules name are added to them
     * @param creatureTypes the creature type names to check, for example MONSTER and WATER_CREATURE
     */
    public static Report compare(List<SpawnRuleSpec> table, JsonArray fileRules, Collection<String> biomes,
                                 Collection<String> creatureTypes) {
        List<FileRule> fromFile = new ArrayList<FileRule>();
        Map<String, Integer> unsupported = new java.util.TreeMap<String, Integer>();
        for (JsonElement element : fileRules) {
            if (element.isJsonObject()) {
                fromFile.add(parse(element.getAsJsonObject(), unsupported));
            }
        }
        List<FileRule> fromTable = new ArrayList<FileRule>();
        for (SpawnRuleSpec spec : table) {
            fromTable.add(parse(spec.toJson(), new java.util.TreeMap<String, Integer>()));
        }

        Set<Integer> dimensions = new TreeSet<Integer>();
        dimensions.add(OTHER_DIMENSION);
        Set<String> allBiomes = new TreeSet<String>(biomes);
        allBiomes.add(OTHER_BIOME);
        for (FileRule rule : fromFile) {
            if (rule.dimensions != null) {
                dimensions.addAll(rule.dimensions);
            }
            if (rule.biomes != null) {
                allBiomes.addAll(rule.biomes);
            }
        }
        for (SpawnRuleSpec spec : table) {
            dimensions.add(spec.dimension);
            if (spec.biomes != null) {
                allBiomes.addAll(spec.biomes);
            }
        }

        List<String> differences = new ArrayList<String>();
        int checked = 0;
        int different = 0;
        for (int dimension : dimensions) {
            for (String biome : allBiomes) {
                for (String type : creatureTypes) {
                    checked++;
                    List<String> expected = events(fromFile, dimension, biome, type);
                    List<String> actual = new ArrayList<String>();
                    for (int i = 0; i < table.size(); i++) {
                        if (table.get(i).appliesTo(dimension, biome, type)) {
                            actual.addAll(fromTable.get(i).removes);
                            actual.addAll(fromTable.get(i).adds);
                        }
                    }
                    if (!same(expected, actual)) {
                        different++;
                        if (differences.size() < MAX_REPORTED) {
                            differences.add(describe(dimension, biome, type, expected, actual));
                        }
                    }
                }
            }
        }
        if (different > differences.size()) {
            differences.add("... and " + (different - differences.size()) + " more queries differ");
        }
        List<String> unsupportedList = new ArrayList<String>();
        for (Map.Entry<String, Integer> entry : unsupported.entrySet()) {
            unsupportedList.add(entry.getKey() + " (" + entry.getValue() + " rules)");
        }
        return new Report(checked, differences, unsupportedList, fromFile.size(), table.size());
    }

    private static List<String> events(List<FileRule> rules, int dimension, String biome, String type) {
        int queryDimension = dimension;
        List<String> out = new ArrayList<String>();
        for (FileRule rule : rules) {
            if (rule.appliesTo(queryDimension, biome, type)) {
                out.addAll(rule.removes);
                out.addAll(rule.adds);
            }
        }
        return out;
    }

    /** The same events; the order only matters when a removal is involved, as it acts on what came before. */
    private static boolean same(List<String> expected, List<String> actual) {
        boolean removal = false;
        for (String event : expected) {
            removal |= event.startsWith("remove");
        }
        for (String event : actual) {
            removal |= event.startsWith("remove");
        }
        if (removal) {
            return expected.equals(actual);
        }
        List<String> a = new ArrayList<String>(expected);
        List<String> b = new ArrayList<String>(actual);
        Collections.sort(a);
        Collections.sort(b);
        return a.equals(b);
    }

    private static String describe(int dimension, String biome, String type, List<String> expected,
                                   List<String> actual) {
        List<String> onlyFile = new ArrayList<String>(expected);
        List<String> onlyTable = new ArrayList<String>(actual);
        for (String event : actual) {
            onlyFile.remove(event);
        }
        for (String event : expected) {
            onlyTable.remove(event);
        }
        String where = "dimension " + (dimension == OTHER_DIMENSION ? "(any other)" : String.valueOf(dimension))
                + ", biome " + biome + ", " + type;
        if (onlyFile.isEmpty() && onlyTable.isEmpty()) {
            return where + ": same entries in a different order around a removal";
        }
        return where + ": only in potentialspawn.json " + abbreviate(onlyFile) + "; only in the table "
                + abbreviate(onlyTable);
    }

    private static String abbreviate(List<String> events) {
        if (events.size() <= 3) {
            return events.toString();
        }
        return events.subList(0, 3) + " and " + (events.size() - 3) + " more";
    }

    private static FileRule parse(JsonObject json, Map<String, Integer> unsupported) {
        FileRule rule = new FileRule();
        if (json.has("dimension")) {
            rule.dimensions = new HashSet<Integer>();
            for (JsonElement value : asList(json.get("dimension"))) {
                rule.dimensions.add(value.getAsInt());
            }
        }
        if (json.has("biomereg")) {
            rule.biomes = new HashSet<String>();
            for (JsonElement value : asList(json.get("biomereg"))) {
                rule.biomes.add(id(value.getAsString()));
            }
        }
        if (json.has("creaturetype")) {
            rule.creatureType = json.get("creaturetype").getAsString();
        }

        StringBuilder conditions = new StringBuilder();
        if (json.has("structure")) {
            conditions.append(" structure=").append(json.get("structure").getAsString());
        }
        if (json.has("maxheight")) {
            conditions.append(" maxheight=").append(json.get("maxheight").getAsInt());
        }
        if (json.has("block")) {
            conditions.append(" block=").append(sortedIds(json.get("block")));
        }
        if (json.has("maxcount")) {
            conditions.append(" maxcount=").append(countText(json.get("maxcount")));
        }
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            if (!HANDLED.contains(entry.getKey())) {
                conditions.append(' ').append(entry.getKey()).append('=').append(entry.getValue());
                Integer seen = unsupported.get(entry.getKey());
                unsupported.put(entry.getKey(), seen == null ? 1 : seen + 1);
            }
        }
        rule.conditions = conditions.toString();

        if (json.has("remove")) {
            rule.removes.add("remove " + sortedIds(json.get("remove")));
        }
        if (json.has("mobs") && json.get("mobs").isJsonArray()) {
            for (JsonElement mobElement : json.get("mobs").getAsJsonArray()) {
                if (!mobElement.isJsonObject()) {
                    continue;
                }
                JsonObject mob = mobElement.getAsJsonObject();
                int weight = mob.has("weight") ? mob.get("weight").getAsInt() : 1;
                int min = mob.has("groupcountmin") ? mob.get("groupcountmin").getAsInt() : 1;
                int max = mob.has("groupcountmax") ? mob.get("groupcountmax").getAsInt() : Math.max(min, 1);
                if (!mob.has("mob")) {
                    continue;
                }
                for (JsonElement name : asList(mob.get("mob"))) {
                    rule.adds.add("add " + id(name.getAsString()) + " weight=" + weight + " group=" + min + "-" + max
                            + rule.conditions);
                }
            }
        }
        return rule;
    }

    private static String countText(JsonElement element) {
        if (!element.isJsonObject()) {
            return element.toString();
        }
        JsonObject object = element.getAsJsonObject();
        StringBuilder text = new StringBuilder("{");
        text.append("amount=").append(object.has("amount") ? object.get("amount").getAsInt() : -1);
        if (object.has("mob")) {
            text.append(" mob=").append(sortedIds(object.get("mob")));
        }
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            String key = entry.getKey();
            if (!key.equals("amount") && !key.equals("mob")) {
                text.append(' ').append(key).append('=').append(entry.getValue());
            }
        }
        return text.append('}').toString();
    }

    private static String sortedIds(JsonElement element) {
        List<String> ids = new ArrayList<String>();
        for (JsonElement value : asList(element)) {
            ids.add(value.isJsonPrimitive() ? id(value.getAsString()) : value.toString());
        }
        Collections.sort(ids);
        return new LinkedHashSet<String>(ids).toString();
    }

    private static List<JsonElement> asList(JsonElement element) {
        List<JsonElement> out = new ArrayList<JsonElement>();
        if (element.isJsonArray()) {
            for (JsonElement value : element.getAsJsonArray()) {
                out.add(value);
            }
        } else {
            out.add(element);
        }
        return out;
    }

    /** The registry id as Minecraft normalises it: lower case, with the minecraft namespace when none is given. */
    static String id(String raw) {
        String lower = raw.trim().toLowerCase(Locale.ROOT);
        return lower.indexOf(':') < 0 ? "minecraft:" + lower : lower;
    }
}
