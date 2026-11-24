package com.apocollis.aqtweaks.thaumcraft.battlemage;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** The pure rolls behind the Battlemage drops (plan 13 to 13f). No Minecraft types, unit tested. */
public final class DropRules {

    /** Worn-slot weights of the gear roll (plan 13c): sword 1, chestplate 1, caster 2, helmet 2, leggings 2, boots 2. */
    public static double defaultGearWeight(String slot) {
        switch (slot) {
            case "mainhand":
            case "chest":
                return 1.0D;
            default:
                return 2.0D;
        }
    }

    private DropRules() {}

    /** A table chance raised by Looting, capped at 1. */
    public static double lootedChance(double base, int looting, double perLevel) {
        return Math.min(1.0D, Math.max(0.0D, base) + Math.max(0, looting) * Math.max(0.0D, perLevel));
    }

    /** A count in {@code min..max}, with Looting adding 1 to the maximum per level. */
    public static int count(int min, int max, int looting, Random rnd) {
        int lo = Math.max(0, min);
        int hi = Math.max(lo, max) + Math.max(0, looting);
        return lo + (hi > lo ? rnd.nextInt(hi - lo + 1) : 0);
    }

    /** How many of a kind drop: 1, plus one more with {@code perLooting * looting} chance, never above {@code cap}. */
    public static int pieces(double perLooting, int looting, int cap, Random rnd) {
        int n = 1;
        if (cap > 1 && looting > 0 && rnd.nextDouble() < Math.min(1.0D, perLooting * looting)) {
            n++;
        }
        return Math.min(n, Math.max(1, cap));
    }

    /** {@code k} distinct indices picked by weight without replacement; zero or negative weights are never picked. */
    public static List<Integer> pickDistinct(double[] weights, int k, Random rnd) {
        List<Integer> left = new ArrayList<>();
        for (int i = 0; i < weights.length; i++) {
            if (weights[i] > 0.0D) {
                left.add(i);
            }
        }
        List<Integer> out = new ArrayList<>();
        while (out.size() < k && !left.isEmpty()) {
            double total = 0.0D;
            for (int idx : left) {
                total += weights[idx];
            }
            double roll = rnd.nextDouble() * total;
            int chosen = left.get(left.size() - 1);
            for (int idx : left) {
                roll -= weights[idx];
                if (roll < 0.0D) {
                    chosen = idx;
                    break;
                }
            }
            out.add(chosen);
            left.remove(Integer.valueOf(chosen));
        }
        return out;
    }

    /** Focus drop weight (plan 13f): an explicit override, else fixed-slot, high-tier (Tier 2 only) or rolled. */
    public static double focusWeight(Double override, boolean fixedSlot, int minTier, double fixed, double rolled, double high) {
        if (override != null) {
            return override;
        }
        if (fixedSlot) {
            return fixed;
        }
        return minTier >= 2 ? high : rolled;
    }
}
