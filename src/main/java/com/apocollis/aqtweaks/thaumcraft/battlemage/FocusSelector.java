package com.apocollis.aqtweaks.thaumcraft.battlemage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Stage 1 focus selection (rule 9 of plan 3.2, roles from 3.16): filter by cooldown, range and line of sight, score by
 * role and the target's state, variety, weighted pick among the best. Pure and unit tested.
 */
public final class FocusSelector {

    /** The roles the stage 1 AI casts. Heal, peel, summon and the AI-performed moves come in later stages. */
    public static final Set<String> STAGE1_ROLES = new HashSet<>(Arrays.asList(
            "ranged_primary", "ranged_heavy", "bolt", "bolt_heavy", "debuff", "aoe", "area_denial"));

    /** One loadout focus as the selector sees it. */
    public static final class Entry {
        public final String id;
        public final String role;
        public final double minRange;
        public final double maxRange;
        public final boolean requireLos;
        public final double weight;
        public final String opener;
        public final List<String> followUp;
        /** Authored complexity: the cast budget cost is this times the cost factor. 0 costs nothing. */
        public int complexity;
        /** Bats one cast of a summon_upkeep focus spawns. */
        public int summonCount = 1;
        /** A cloud focus waits while the mage still owns a cloud. */
        public boolean refireOnExpire;

        public Entry(String id, String role, double minRange, double maxRange, boolean requireLos, double weight,
                String opener, List<String> followUp) {
            this.id = id;
            this.role = role;
            this.minRange = minRange;
            this.maxRange = maxRange;
            this.requireLos = requireLos;
            this.weight = weight;
            this.opener = opener;
            this.followUp = followUp;
        }
    }

    /** What the selector needs to know about the moment. */
    public static final class Context {
        public double distance;
        public boolean hasLos;
        public long tick;
        /** Target states present right now: stunned, slowed, wet, burning, debuffed. */
        public Set<String> targetStates = new HashSet<>();
        public String lastFocusId;
        public double aoeMinRange = 14.0D;
        public Random random = new Random();
        /** Cast budget (plan 3.11 item 1): current energy, its maximum, the cost factor and whether the mage is holding back. */
        public double energy = Double.POSITIVE_INFINITY;
        public double maxEnergy = Double.POSITIVE_INFINITY;
        public double costPerComplexity = 1.0D;
        public boolean holding;
        /** Clouds this mage owns right now (for the cloud refire rule). */
        public int ownedClouds;
        /** True until the first cast of the fight: openers score higher. */
        public boolean opening;
    }

    private FocusSelector() {}

    /** @return the index into {@code entries} to cast, or -1 */
    public static int pick(List<Entry> entries, long[] readyAt, Context ctx) {
        List<Integer> idx = new ArrayList<>();
        List<Double> scores = new ArrayList<>();
        double best = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < entries.size(); i++) {
            Entry e = entries.get(i);
            if (!STAGE1_ROLES.contains(e.role)) {
                continue;
            }
            if (readyAt[i] > ctx.tick) {
                continue;
            }
            if (ctx.holding) {
                continue;
            }
            if (Math.min(e.complexity * ctx.costPerComplexity, ctx.maxEnergy) > ctx.energy) {
                continue;
            }
            if (ctx.distance < e.minRange || ctx.distance > e.maxRange) {
                continue;
            }
            if (e.requireLos && !ctx.hasLos) {
                continue;
            }
            double score = score(e, ctx);
            if (score <= 0.0D) {
                continue;
            }
            idx.add(i);
            scores.add(score);
            best = Math.max(best, score);
        }
        if (idx.isEmpty()) {
            return -1;
        }
        double sum = 0.0D;
        List<Integer> top = new ArrayList<>();
        List<Double> topScores = new ArrayList<>();
        for (int k = 0; k < idx.size(); k++) {
            if (scores.get(k) >= best - 1.0D) {
                top.add(idx.get(k));
                topScores.add(scores.get(k));
                sum += scores.get(k);
            }
        }
        double roll = ctx.random.nextDouble() * sum;
        for (int k = 0; k < top.size(); k++) {
            roll -= topScores.get(k);
            if (roll <= 0.0D) {
                return top.get(k);
            }
        }
        return top.get(top.size() - 1);
    }

    /**
     * Rule 6 of plan 3.2 (summon upkeep): while fewer Spell Bats are alive than wanted, the ready {@code summon_upkeep}
     * focus is cast at once. No range, line of sight or budget test; only its own cooldown applies. When several are
     * ready the one that spawns the most bats wins.
     *
     * @return the index into {@code entries}, or -1
     */
    public static int pickUpkeep(List<Entry> entries, long[] readyAt, long tick, int activeBats, int desiredBats) {
        if (!SummonUpkeep.needed(activeBats, desiredBats)) {
            return -1;
        }
        int best = -1;
        for (int i = 0; i < entries.size(); i++) {
            Entry e = entries.get(i);
            if (!"summon_upkeep".equals(e.role) || readyAt[i] > tick) {
                continue;
            }
            if (best < 0 || e.summonCount > entries.get(best).summonCount) {
                best = i;
            }
        }
        return best;
    }

    /** Largest bat count one cast of any summon_upkeep focus in the loadout spawns, 0 when it carries none. */
    public static int summonCastSize(List<Entry> entries) {
        int size = 0;
        for (Entry e : entries) {
            if ("summon_upkeep".equals(e.role)) {
                size = Math.max(size, e.summonCount);
            }
        }
        return size;
    }

    static double score(Entry e, Context ctx) {
        double s;
        switch (e.role) {
            case "aoe":
            case "area_denial":
                if (ctx.distance <= ctx.aoeMinRange) {
                    return 0.0D;
                }
                if (e.refireOnExpire && ctx.ownedClouds > 0) {
                    return 0.0D;
                }
                s = 6.0D;
                break;
            case "bolt":
                s = 4.5D;
                break;
            case "ranged_primary":
                s = 4.0D;
                break;
            case "debuff":
                s = ctx.targetStates.contains("debuffed") ? 2.0D : 5.0D;
                break;
            case "bolt_heavy":
            case "ranged_heavy":
                s = 2.0D + (hasFollowUp(e, ctx) ? 4.0D : 0.0D);
                break;
            default:
                return 0.0D;
        }
        if (e.opener != null && !anyFollowUpState(ctx)) {
            s += 1.0D;
            if (ctx.opening) {
                s += 3.0D;
            }
        }
        if (e.id.equals(ctx.lastFocusId)) {
            s -= 1.5D;
        }
        return Math.max(0.0D, s) * e.weight;
    }

    private static boolean hasFollowUp(Entry e, Context ctx) {
        for (String state : e.followUp) {
            if (ctx.targetStates.contains(state)) {
                return true;
            }
        }
        return false;
    }

    private static boolean anyFollowUpState(Context ctx) {
        return ctx.targetStates.contains("stunned") || ctx.targetStates.contains("slowed")
                || ctx.targetStates.contains("wet") || ctx.targetStates.contains("burning");
    }
}
