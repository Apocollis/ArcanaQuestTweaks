package com.apocollis.aqtweaks.thaumcraft.studious;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Pure rules for the Scholar's Studious perk: which research cards are offered and how much they add. Works on
 * the plain category totals map of Thaumcraft's {@code ResearchTableData}, so it needs no Minecraft types.
 */
public final class StudiousMath {

    private StudiousMath() {}

    /** The category with the highest total above zero; ties go to the first key in map order. Null when none. */
    public static String leadingCategory(Map<String, Integer> totals) {
        if (totals == null) {
            return null;
        }
        String best = null;
        int bestTotal = 0;
        for (Map.Entry<String, Integer> e : totals.entrySet()) {
            int value = e.getValue() == null ? 0 : e.getValue();
            if (value > bestTotal) {
                best = e.getKey();
                bestTotal = value;
            }
        }
        return best;
    }

    /** Eureka is offered once the leading category has reached {@code threshold}. */
    public static boolean eurekaEligible(Map<String, Integer> totals, int threshold) {
        String lead = leadingCategory(totals);
        return lead != null && totals.get(lead) >= Math.max(1, threshold);
    }

    /**
     * Categories that already carry progress, strongest first (ties by name), at most {@code max}. Cross-Reference
     * adds its bonus to each of these.
     */
    public static List<String> crossReferenceTargets(Map<String, Integer> totals, int max) {
        List<String> keys = new ArrayList<>();
        if (totals == null || max <= 0) {
            return keys;
        }
        for (Map.Entry<String, Integer> e : totals.entrySet()) {
            if (e.getValue() != null && e.getValue() > 0) {
                keys.add(e.getKey());
            }
        }
        keys.sort((a, b) -> {
            int byTotal = Integer.compare(totals.get(b), totals.get(a));
            return byTotal != 0 ? byTotal : a.compareTo(b);
        });
        return keys.size() > max ? new ArrayList<>(keys.subList(0, max)) : keys;
    }

    /** Cross-Reference is offered once at least {@code minCategories} categories carry progress. */
    public static boolean crossReferenceEligible(Map<String, Integer> totals, int minCategories) {
        return crossReferenceTargets(totals, Integer.MAX_VALUE).size() >= Math.max(1, minCategories);
    }

    /** Steady Focus: a cast cooldown of {@code ticks} shortened by {@code cut} (0 to 0.9). Never below 1 tick. */
    public static int scaleCooldown(int ticks, double cut) {
        if (ticks <= 0 || cut <= 0.0D) {
            return ticks;
        }
        double kept = 1.0D - Math.min(0.9D, cut);
        return Math.max(1, (int) Math.round(ticks * kept));
    }
}
