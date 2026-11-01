package com.apocollis.aqtweaks.spawning.rules;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The rows of {@code spawn_gear.json}: the first matching row gives the mob its whole loadout. */
public final class GearTable {
    /** What a mob is given: slot name to item, and the row it came from. */
    public static final class Loadout {
        static final Loadout NONE = new Loadout(Collections.<String, String>emptyMap(), -1, false);

        private final Map<String, String> slots;
        private final int row;
        private final boolean usedRandom;

        Loadout(Map<String, String> slots, int row, boolean usedRandom) {
            this.slots = Collections.unmodifiableMap(slots);
            this.row = row;
            this.usedRandom = usedRandom;
        }

        /** Slot name ({@code hand}, {@code offhand}, {@code head}, {@code chest}, {@code legs}, {@code feet}) to item id. */
        public Map<String, String> slots() {
            return slots;
        }

        public int row() {
            return row;
        }

        public boolean matched() {
            return row >= 0;
        }

        public boolean usedRandom() {
            return usedRandom;
        }
    }

    private final List<ConditionRow> matchers;
    private final List<Map<String, String>> loadouts;

    private GearTable(List<ConditionRow> matchers, List<Map<String, String>> loadouts) {
        this.matchers = matchers;
        this.loadouts = loadouts;
    }

    public static GearTable compile(List<RuleRow> rules, Map<String, List<String>> categories) {
        List<JsonObject> json = new ArrayList<JsonObject>();
        for (RuleRow rule : rules) {
            json.add(rule.json);
        }
        return compileJson(json, categories);
    }

    public static GearTable compileJson(List<JsonObject> rules, Map<String, List<String>> categories) {
        List<ConditionRow> matchers = new ArrayList<ConditionRow>();
        List<Map<String, String>> loadouts = new ArrayList<Map<String, String>>();
        for (JsonObject json : rules) {
            matchers.add(ConditionRow.matcher(json, matchers.size(), categories));
            Map<String, String> slots = new LinkedHashMap<String, String>();
            for (String slot : RuleSchema.SLOT_KEYS) {
                if (json.has(slot)) {
                    slots.put(slot, json.get(slot).getAsString());
                }
            }
            loadouts.add(slots);
        }
        return new GearTable(matchers, loadouts);
    }

    public int size() {
        return matchers.size();
    }

    public Loadout decide(SpawnQuery query) {
        boolean usedRandom = false;
        for (int i = 0; i < matchers.size(); i++) {
            ConditionRow row = matchers.get(i);
            usedRandom |= row.rollDecides(query);
            if (row.matches(query)) {
                return new Loadout(loadouts.get(i), i, usedRandom);
            }
        }
        return usedRandom ? new Loadout(Collections.<String, String>emptyMap(), -1, true) : Loadout.NONE;
    }
}
