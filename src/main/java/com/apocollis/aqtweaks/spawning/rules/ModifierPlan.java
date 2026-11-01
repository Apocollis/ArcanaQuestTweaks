package com.apocollis.aqtweaks.spawning.rules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * What the modifiers table gives one mob: for each stat the multiplier and addend from the first matching row that
 * sets it, the potions of every matching row, and whether any matching row makes it angry.
 */
public final class ModifierPlan {
    /** The stats a row can change, as the prefix of its {@code ...multiply} and {@code ...add} keys. */
    public static final List<String> STATS = Collections.unmodifiableList(java.util.Arrays.asList(
            "health", "damage", "speed", "armor", "armortoughness", "knockbackresistance", "followrange", "flyingspeed"));

    static final ModifierPlan EMPTY = new ModifierPlan(new LinkedHashMap<String, double[]>(), new ArrayList<String>(),
            false, new ArrayList<Integer>(), false);

    private final Map<String, double[]> stats;
    private final List<String> potions;
    private final boolean angry;
    private final List<Integer> rows;
    private final boolean usedRandom;

    ModifierPlan(Map<String, double[]> stats, List<String> potions, boolean angry, List<Integer> rows,
                 boolean usedRandom) {
        this.stats = stats;
        this.potions = Collections.unmodifiableList(potions);
        this.angry = angry;
        this.rows = Collections.unmodifiableList(rows);
        this.usedRandom = usedRandom;
    }

    /** The stats this plan changes. */
    public Set<String> stats() {
        return Collections.unmodifiableSet(stats.keySet());
    }

    /** {@code base * multiply + add} for a stat the plan sets. */
    public double expected(String stat, double base) {
        double[] change = stats.get(stat);
        return change == null ? base : base * change[0] + change[1];
    }

    /** Potions as {@code id,duration,amplifier}, in row order, without repeats. */
    public List<String> potions() {
        return potions;
    }

    public boolean angry() {
        return angry;
    }

    /** The rows that contributed, in order. */
    public List<Integer> rows() {
        return rows;
    }

    /** True when a row's random chance decided whether it applied, so the plan may differ run to run. */
    public boolean usedRandom() {
        return usedRandom;
    }

    public boolean isEmpty() {
        return stats.isEmpty() && potions.isEmpty() && !angry;
    }

    @Override
    public String toString() {
        return "rows " + rows + " stats " + stats.keySet() + " potions " + potions + (angry ? " angry" : "");
    }
}
