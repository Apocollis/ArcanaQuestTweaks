package com.apocollis.aqtweaks.spawning.rules;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The rows of {@code spawn_modifiers.json}. Every matching row contributes until a row with {@code stop} has applied:
 * each stat comes from the first matching row that sets it, potions from every matching row, and {@code angry} applies
 * if any matching row has it. {@code generate} rows are not built yet and are skipped, with a note.
 */
public final class ModifierTable {
    private static final class Row {
        final ConditionRow match;
        final JsonObject json;
        final boolean stop;

        Row(ConditionRow match, JsonObject json) {
            this.match = match;
            this.json = json;
            this.stop = json.has("stop") && json.get("stop").getAsBoolean();
        }
    }

    private final List<Row> rows;
    private final List<String> notes;

    private ModifierTable(List<Row> rows, List<String> notes) {
        this.rows = Collections.unmodifiableList(rows);
        this.notes = Collections.unmodifiableList(notes);
    }

    public static ModifierTable compile(List<RuleRow> rules, Map<String, List<String>> categories) {
        List<JsonObject> json = new ArrayList<JsonObject>();
        List<String> notes = new ArrayList<String>();
        for (RuleRow rule : rules) {
            if (rule.json.has("generate")) {
                notes.add("line " + rule.line + ": 'generate' " + rule.json.get("generate").getAsString()
                        + " rows are not built yet and were skipped");
                continue;
            }
            json.add(rule.json);
        }
        ModifierTable table = compileJson(json, categories);
        notes.addAll(table.notes);
        return new ModifierTable(table.rows, notes);
    }

    public static ModifierTable compileJson(List<JsonObject> rules, Map<String, List<String>> categories) {
        List<Row> rows = new ArrayList<Row>();
        for (JsonObject json : rules) {
            rows.add(new Row(ConditionRow.matcher(json, rows.size(), categories), json));
        }
        return new ModifierTable(rows, new ArrayList<String>());
    }

    public int size() {
        return rows.size();
    }

    public List<String> notes() {
        return notes;
    }

    public ModifierPlan decide(SpawnQuery query) {
        Map<String, double[]> stats = null;
        List<String> potions = null;
        List<Integer> used = null;
        boolean angry = false;
        boolean usedRandom = false;
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            usedRandom |= row.match.rollDecides(query);
            if (!row.match.matches(query)) {
                continue;
            }
            if (stats == null) {
                stats = new LinkedHashMap<String, double[]>();
                potions = new ArrayList<String>();
                used = new ArrayList<Integer>();
            }
            used.add(i);
            for (String stat : ModifierPlan.STATS) {
                if (!stats.containsKey(stat) && (row.json.has(stat + "multiply") || row.json.has(stat + "add"))) {
                    stats.put(stat, new double[] {number(row.json, stat + "multiply", 1.0), number(row.json, stat + "add", 0.0)});
                }
            }
            if (row.json.has("potion")) {
                JsonElement value = row.json.get("potion");
                if (value.isJsonArray()) {
                    for (JsonElement each : value.getAsJsonArray()) {
                        addPotion(potions, each.getAsString());
                    }
                } else {
                    addPotion(potions, value.getAsString());
                }
            }
            angry |= row.json.has("angry") && row.json.get("angry").getAsBoolean();
            if (row.stop) {
                break;
            }
        }
        if (stats == null) {
            return usedRandom ? new ModifierPlan(new LinkedHashMap<String, double[]>(), new ArrayList<String>(), false,
                    new ArrayList<Integer>(), true) : ModifierPlan.EMPTY;
        }
        return new ModifierPlan(stats, potions, angry, used, usedRandom);
    }

    private static void addPotion(List<String> potions, String potion) {
        if (!potions.contains(potion)) {
            potions.add(potion);
        }
    }

    private static double number(JsonObject json, String key, double fallback) {
        return json.has(key) ? json.get(key).getAsDouble() : fallback;
    }
}
