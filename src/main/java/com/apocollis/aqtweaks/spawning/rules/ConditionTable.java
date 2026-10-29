package com.apocollis.aqtweaks.spawning.rules;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * The rows of {@code spawn_conditions.json}, ready to decide a spawn. At the spawn check every row is walked in order;
 * at join only the rows that run at both moments. The first row that matches decides, and nothing below it is looked
 * at. {@code generate} rows are not built yet and are skipped, with a note.
 */
public final class ConditionTable {
    public enum Phase {
        SPAWN, JOIN
    }

    private final List<ConditionRow> rows;
    private final List<String> notes;

    private ConditionTable(List<ConditionRow> rows, List<String> notes) {
        this.rows = Collections.unmodifiableList(rows);
        this.notes = Collections.unmodifiableList(notes);
    }

    /** @param categories biome category name to its biome ids, for {@code biomecat} */
    public static ConditionTable compile(List<RuleRow> rules, Map<String, List<String>> categories) {
        List<ConditionRow> rows = new ArrayList<ConditionRow>();
        List<String> notes = new ArrayList<String>();
        for (RuleRow rule : rules) {
            JsonObject json = rule.json;
            if (json.has("generate")) {
                notes.add("line " + rule.line + ": 'generate' " + json.get("generate").getAsString()
                        + " rows are not built yet and were skipped");
                continue;
            }
            rows.add(ConditionRow.of(json, rows.size(), categories));
        }
        return new ConditionTable(rows, notes);
    }

    /** Compiles rules given as plain JSON objects (in-memory sources, tests). */
    public static ConditionTable compileJson(List<JsonObject> rules, Map<String, List<String>> categories) {
        List<ConditionRow> rows = new ArrayList<ConditionRow>();
        for (JsonObject json : rules) {
            rows.add(ConditionRow.of(json, rows.size(), categories));
        }
        return new ConditionTable(rows, new ArrayList<String>());
    }

    public int size() {
        return rows.size();
    }

    public List<String> notes() {
        return notes;
    }

    public Decision decide(Phase phase, SpawnQuery query) {
        boolean usedRandom = false;
        for (ConditionRow row : rows) {
            if (phase == Phase.JOIN && !row.bothPhases) {
                continue;
            }
            usedRandom |= row.random;
            if (row.matches(query)) {
                return new Decision(row.result, row.index, usedRandom);
            }
        }
        return usedRandom ? new Decision(null, -1, true) : Decision.NONE;
    }
}
