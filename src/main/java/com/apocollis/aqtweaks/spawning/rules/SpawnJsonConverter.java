package com.apocollis.aqtweaks.spawning.rules;

import com.apocollis.aqtweaks.spawning.table.LenientJson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Turns In Control's {@code spawn.json} into the three rule files. Each rule is split into the part that decides
 * whether a mob may spawn (conditions), the part that changes a mob (modifiers, each with {@code stop} so the first
 * version decides as today) and the part that equips it (gear). Order is kept, a rule's {@code onjoin} flag becomes
 * its {@code phase}, and anything that cannot be carried over is listed in the report.
 */
public final class SpawnJsonConverter {
    private static final List<String> MATCH = Arrays.asList("mob", "mod", "dimension", "minheight", "maxheight",
            "seesky", "mintime", "maxtime", "weather", "structure", "biomereg", "hostile", "random");
    private static final List<String> GATE_ONLY = Arrays.asList("spawner", "notcolliding", "gamestage",
            "minplayerdist", "maxplayerdist", "mincount", "maxcount");
    private static final List<String> GEAR_MATCH = Arrays.asList("mob", "dimension", "minheight", "maxheight",
            "seesky", "weather", "random");
    private static final Map<String, String> GEAR_SLOTS = new LinkedHashMap<String, String>();

    static {
        GEAR_SLOTS.put("helditem", "hand");
        GEAR_SLOTS.put("helditemoffhand", "offhand");
        GEAR_SLOTS.put("armorhelmet", "head");
        GEAR_SLOTS.put("armorchest", "chest");
        GEAR_SLOTS.put("armorlegs", "legs");
        GEAR_SLOTS.put("armorboots", "feet");
    }

    /** One converted row, with the comments that stood above its source rule. */
    public static final class Row {
        public final List<String> comments;
        public final JsonObject json;
        public final int sourceLine;

        Row(List<String> comments, JsonObject json, int sourceLine) {
            this.comments = comments;
            this.json = json;
            this.sourceLine = sourceLine;
        }
    }

    /** The three files' rows and the notes about what was changed or could not be carried over. */
    public static final class Converted {
        public final List<Row> conditions = new ArrayList<Row>();
        public final List<Row> modifiers = new ArrayList<Row>();
        public final List<Row> gear = new ArrayList<Row>();
        public final List<String> report = new ArrayList<String>();
        public int sourceRules;

        public String conditionsText() {
            return text(conditions);
        }

        public String modifiersText() {
            return text(modifiers);
        }

        public String gearText() {
            return text(gear);
        }
    }

    private SpawnJsonConverter() {
    }

    public static Converted convert(String spawnJson, List<RuleProblem> problems) {
        Converted result = new Converted();
        List<RawRule> raws = RuleScanner.scan(spawnJson, "spawn.json", problems);
        List<JsonObject> rules = new ArrayList<JsonObject>();
        for (RawRule raw : raws) {
            try {
                rules.add(LenientJson.parse(raw.text).getAsJsonObject());
            } catch (RuntimeException malformed) {
                problems.add(new RuleProblem("spawn.json", raw.line, "malformed rule: " + malformed.getMessage()));
                rules.add(new JsonObject());
            }
        }
        result.sourceRules = rules.size();

        // A rule with a result and nothing to change is a gate. Rules that both gate and change a mob only need
        // their gate row when a gate comes after them; later, nothing is left for the gate to shield.
        int lastGate = -1;
        for (int i = 0; i < rules.size(); i++) {
            JsonObject r = rules.get(i);
            if (r.has("result") && !hasAction(r) && !hasGear(r)) {
                lastGate = i;
            }
        }

        Set<String> known = new HashSet<String>();
        known.addAll(MATCH);
        known.addAll(GATE_ONLY);
        known.addAll(RuleSchema.ACTION_KEYS);
        known.addAll(GEAR_SLOTS.keySet());
        known.add("result");
        known.add("onjoin");

        int shieldless = 0;
        for (int i = 0; i < rules.size(); i++) {
            JsonObject r = rules.get(i);
            RawRule raw = raws.get(i);
            String where = "line " + raw.line;
            boolean action = hasAction(r);
            boolean gear = hasGear(r);
            boolean gate = r.has("result");
            boolean onJoin = r.has("onjoin") && r.get("onjoin").getAsBoolean();

            for (Map.Entry<String, JsonElement> entry : r.entrySet()) {
                if (!known.contains(entry.getKey())) {
                    result.report.add(where + ": '" + entry.getKey() + "' has no equivalent and was dropped");
                }
            }
            if (!gate && !action && !gear) {
                result.report.add(where + ": the rule has no result, stat, potion or gear, so nothing was written");
                continue;
            }

            if (gate && (!(action || gear) || i < lastGate)) {
                JsonObject row = new JsonObject();
                copy(r, row, MATCH);
                copy(r, row, GATE_ONLY);
                row.add("result", r.get("result"));
                if (!onJoin) {
                    row.addProperty("phase", "spawn");
                }
                result.conditions.add(new Row(raw.comments, row, raw.line));
            } else if (gate && action) {
                shieldless++;
            }

            if (action) {
                JsonObject row = new JsonObject();
                copy(r, row, MATCH);
                for (String key : RuleSchema.ACTION_KEYS) {
                    if (r.has(key)) {
                        if (key.equals("potion")) {
                            JsonElement potion = withoutNone(r.get(key));
                            if (potion != null) {
                                row.add(key, potion);
                            }
                        } else {
                            row.add(key, r.get(key));
                        }
                    }
                }
                row.addProperty("stop", true);
                result.modifiers.add(new Row(raw.comments, row, raw.line));
                if (r.has("random") && gate && i < lastGate) {
                    result.report.add(where + ": 'random' is in both the condition row and the modifier row, so the"
                            + " chance is rolled twice");
                }
                if ((r.has("gamestage") || r.has("maxcount") || r.has("mincount")) && !gate) {
                    result.report.add(where + ": gamestage/count are conditions on a rule with no result; they were"
                            + " dropped from its modifier row");
                }
            }

            if (gear) {
                if (!r.has("mob")) {
                    result.report.add(where + ": gear rule without 'mob' was dropped");
                } else {
                    JsonObject row = new JsonObject();
                    copy(r, row, GEAR_MATCH);
                    for (Map.Entry<String, String> slot : GEAR_SLOTS.entrySet()) {
                        if (r.has(slot.getKey())) {
                            row.add(slot.getValue(), r.get(slot.getKey()));
                        }
                    }
                    for (String key : Arrays.asList("mod", "structure", "biomereg", "hostile", "mintime", "maxtime")) {
                        if (r.has(key)) {
                            result.report.add(where + ": gear rows cannot test '" + key + "'; it was dropped");
                        }
                    }
                    result.gear.add(new Row(raw.comments, row, raw.line));
                }
            }
        }
        if (shieldless > 0) {
            result.report.add(shieldless + " rules with a stat or potion and a result had the result left out: no"
                    + " condition rule comes after them, so there was nothing for it to shield");
        }
        moveSpecificAboveBroad(result);
        result.report.add(0, result.sourceRules + " rules -> " + result.conditions.size() + " condition rows, "
                + result.modifiers.size() + " modifier rows, " + result.gear.size() + " gear rows.");
        return result;
    }

    /**
     * A modifier row for one named mob goes above an earlier row whose mob list contains it, so a mob's own buffs are
     * not shadowed by its tier's. (The Crimson archer's buffs above the cultist underground row.)
     */
    private static void moveSpecificAboveBroad(Converted result) {
        List<Row> rows = result.modifiers;
        for (int i = 1; i < rows.size(); i++) {
            JsonObject row = rows.get(i).json;
            if (!row.has("mob") || !row.get("mob").isJsonPrimitive()) {
                continue;
            }
            String mob = row.get("mob").getAsString();
            for (int j = 0; j < i; j++) {
                JsonObject earlier = rows.get(j).json;
                if (earlier.has("mob") && earlier.get("mob").isJsonArray() && contains(earlier.get("mob"), mob)) {
                    Row moved = rows.remove(i);
                    rows.add(j, moved);
                    result.report.add("line " + moved.sourceLine + ": the row for " + mob + " was moved above the row"
                            + " from line " + rows.get(j + 1).sourceLine + ", which also lists it");
                    break;
                }
            }
        }
    }

    private static boolean contains(JsonElement list, String mob) {
        for (JsonElement element : list.getAsJsonArray()) {
            if (element.getAsString().equals(mob)) {
                return true;
            }
        }
        return false;
    }

    private static JsonElement withoutNone(JsonElement potion) {
        if (potion.isJsonArray()) {
            JsonArray kept = new JsonArray();
            for (JsonElement element : potion.getAsJsonArray()) {
                if (!"none".equalsIgnoreCase(element.getAsString())) {
                    kept.add(element);
                }
            }
            return kept.size() == 0 ? null : kept;
        }
        return "none".equalsIgnoreCase(potion.getAsString()) ? null : potion;
    }

    private static boolean hasAction(JsonObject r) {
        for (String key : RuleSchema.ACTION_KEYS) {
            if (r.has(key) && !(key.equals("potion") && withoutNone(r.get(key)) == null)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasGear(JsonObject r) {
        for (String key : GEAR_SLOTS.keySet()) {
            if (r.has(key)) {
                return true;
            }
        }
        return false;
    }

    private static void copy(JsonObject from, JsonObject to, List<String> keys) {
        for (Map.Entry<String, JsonElement> entry : from.entrySet()) {
            if (keys.contains(entry.getKey())) {
                to.add(entry.getKey(), entry.getValue());
            }
        }
    }

    private static String text(List<Row> rows) {
        StringBuilder out = new StringBuilder("[\n");
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            for (String comment : row.comments) {
                out.append("  // ").append(comment).append('\n');
            }
            out.append("  ").append(row.json).append(i < rows.size() - 1 ? "," : "").append('\n');
        }
        return out.append("]\n").toString();
    }
}
